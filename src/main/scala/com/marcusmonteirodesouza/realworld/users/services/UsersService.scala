package com.marcusmonteirodesouza.realworld.users.services

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.marcusmonteirodesouza.realworld.users.domain.User
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json.DefaultJsonProtocol.*
import spray.json.{DefaultJsonProtocol, NullOptions, RootJsonFormat}

import java.time.Clock
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

private final case class RegisterUserRequest(user: RegisterUserRequestUser)

private final case class RegisterUserRequestUser(username: String,
                                                 email: String,
                                                 password: String)

private final case class UserResponse(user: UserResponseUser)

private final case class UserResponseUser(email: String,
                                          token: String,
                                          username: String,
                                          bio: Option[String] = None,
                                          image: Option[String] = None)

private trait JsonSupport
    extends SprayJsonSupport
    with DefaultJsonProtocol
    with NullOptions {
  implicit val registerUserRequestUserFormat
    : RootJsonFormat[RegisterUserRequestUser] = jsonFormat3(
    RegisterUserRequestUser.apply)
  implicit val registerUserRequestFormat: RootJsonFormat[RegisterUserRequest] =
    jsonFormat1(RegisterUserRequest.apply)
  implicit val userFormat: RootJsonFormat[UserResponseUser] = jsonFormat5(
    UserResponseUser.apply)
  implicit val userResponseFormat: RootJsonFormat[UserResponse] = jsonFormat1(
    UserResponse.apply)
}

class UsersService(
    jwtExpiresInSeconds: Int,
    jwtIssuer: String,
    jwtSecretKey: String,
)(implicit sharding: ClusterSharding)
    extends Directives
    with JsonSupport {
  implicit val ec: scala.concurrent.ExecutionContext = ExecutionContext.global

  implicit val timeout: Timeout = 5.seconds

  sharding.init(entity = Entity(User.typeKey)(entityContext =>
    User(entityContext.entityId)))

  val route: Route =
    pathPrefix("users") {
      concat(
        post {
          entity(as[RegisterUserRequest]) {
            request =>
              val userId = UUID.randomUUID.toString

              val user = sharding.entityRefFor(User.typeKey, userId)

              def auxUpdateUser(
                  email: String,
                  username: String,
                  password: String)(replyTo: ActorRef[User.CurrentState]) =
                User.Update(email = Some(email),
                            username = Some(username),
                            password = Some(password),
                            replyTo = replyTo)

              val response = user
                .ask(
                  auxUpdateUser(email = request.user.email,
                                username = request.user.username,
                                password = request.user.password))
                .map {
                  case User.CurrentState(state) =>
                    UserResponse(
                      user = UserResponseUser(email = state.email,
                                              token = jwtEncode(state.id),
                                              username = state.username)
                    )
                }

              complete(response)
          }
        }
      )
    } ~ path("user") {
      pathEndOrSingleSlash {
        get {
          parameters("id".as[String]) { userId =>
            val user = sharding.entityRefFor(User.typeKey, userId)

            def auxGetState()(replyTo: ActorRef[User.CurrentState]) =
              User.GetState(replyTo = replyTo)

            val response = user
              .ask(auxGetState())
              .map {
                case User.CurrentState(state) =>
                  UserResponse(
                    user = UserResponseUser(email = state.email,
                                            token = jwtEncode(state.id),
                                            username = state.username)
                  )
              }

            complete(response)
          }
        }
      }
    }

  private def jwtEncode(userId: String) = {
    implicit val clock: Clock = Clock.systemUTC

    var claim = JwtClaim()
    claim = claim.about(userId)
    claim = claim.expiresIn(seconds = jwtExpiresInSeconds)
    claim = claim.by(jwtIssuer)

    Jwt.encode(claim = claim, jwtSecretKey, JwtAlgorithm.HS512)
  }
}
