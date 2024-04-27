package com.marcusmonteirodesouza.realworld.users.domain

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}
import com.marcusmonteirodesouza.realworld.common.traits.CborSerializable
import io.github.nremond.SecureHash

object User {
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("user")

  sealed trait Command extends CborSerializable
  final case class GetState(replyTo: ActorRef[CurrentState]) extends Command
  final case class Update(email: Option[String] = None,
                          username: Option[String] = None,
                          password: Option[String] = None,
                          bio: Option[String] = None,
                          image: Option[String] = None,
                          replyTo: ActorRef[CurrentState])
      extends Command

  sealed trait Response extends CborSerializable
  final case class CurrentState(state: State) extends Response

  private sealed trait Event extends CborSerializable
  private final case class Updated(state: State) extends Event

  final case class State(id: String,
                         email: String,
                         username: String,
                         passwordHash: String,
                         bio: Option[String],
                         image: Option[String])
      extends CborSerializable
  private object State {
    def empty(id: String): State =
      State(id = id,
            email = "",
            username = "",
            passwordHash = "",
            bio = null,
            image = null)
  }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId(typeKey.name, id),
      emptyState = State.empty(id = id),
      commandHandler = handleCommands,
      eventHandler = handleEvents
    )

  private def handleCommands(state: State,
                             command: Command): ReplyEffect[Event, State] = {
    (state, command) match {
      case (_, GetState(replyTo)) =>
        Effect.none.thenReply(replyTo)(_ => CurrentState(state = state))
      case (state: State, command: Update) => update(state, command)
    }
  }

  private def update(state: State,
                     command: Update): ReplyEffect[Updated, State] = {
    val email = command.email.getOrElse(state.email)
    val username = command.username.getOrElse(state.username)

    val passwordHash =
      if (command.password.isDefined)
        SecureHash.createHash(password = command.password.get)
      else state.passwordHash

    val bio = command.bio.orElse(state.bio)

    val image = command.image.orElse(state.image)

    val updated = Updated(
      state = State(id = state.id,
                    email = email,
                    username = username,
                    passwordHash = passwordHash,
                    bio = bio,
                    image = image))

    Effect
      .persist(updated)
      .thenReply(command.replyTo)(_ => CurrentState(state = updated.state))
  }

  private def handleEvents(state: State, event: Event): State =
    event match {
      case Updated(state) => state
    }
}
