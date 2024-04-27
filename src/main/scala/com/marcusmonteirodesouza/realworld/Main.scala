package com.marcusmonteirodesouza.realworld

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import com.marcusmonteirodesouza.realworld.users.services.UsersService
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success}

object Main extends App {
  private val logger = LoggerFactory.getLogger(Main.getClass.getName)

  implicit val system: ActorSystem[Any] =
    ActorSystem(Behaviors.empty, "realworld-backend-scala-akka")

  implicit val sharding: ClusterSharding = ClusterSharding(system)

  implicit val ec: ExecutionContext = system.executionContext

  private val host = system.settings.config.getString("service.host")
  private val port = system.settings.config.getInt("service.port")

  val jwtExpiresInSeconds =
    system.settings.config.getInt("service.users.jwt.expiresInSeconds")
  val jwtIssuer = system.settings.config.getString("service.users.jwt.issuer")
  val jwtSecretKey =
    system.settings.config.getString("service.users.jwt.secretKey")

  private val secondsToWait = 10

  private val usersService = new UsersService(jwtExpiresInSeconds = jwtExpiresInSeconds,
                                      jwtIssuer = jwtIssuer,
                                      jwtSecretKey = jwtSecretKey)

  private val routes = usersService.route

  private var serverBindingHttp: Option[Http.ServerBinding] = None

  Http()
    .newServerAt(host, port)
    .bind(routes)
    .map(_.addToCoordinatedShutdown(hardTerminationDeadline = secondsToWait.seconds))
    .onComplete {
      case Success(binding) =>
        logger.info(
          "HTTP server online at " + s"http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}")
        serverBindingHttp = Option(binding)
      case Failure(exception) =>
        logger.error("Server could not start!")
        exception.printStackTrace()
        system.terminate()
    }

  Await.result(system.whenTerminated, Duration.Inf)
}
