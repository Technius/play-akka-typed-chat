package actors

import akka.typed._
import akka.typed.scaladsl.Actor

object ChatRoom {
  /**
    * Stores information about all connected users and their usernames.
    */
  type SessionMap = Map[ActorRef[ChatEvent], String]

  /**
    * Retrieves the name associated with the given client output handler, if
    * possible.
    */
  private def nameOf(target: ActorRef[ChatEvent])(sessions: SessionMap): Option[String] =
    sessions.collectFirst { case (r, n) if target == r => n }

  /**
    * Retrieves the client output handler associated with the g iven name, if
    * possible.
    */
  private def actorWithName(name: String)(sessions: SessionMap): Option[ActorRef[ChatEvent]] =
    sessions.collectFirst { case (r, n) if n == name => r }

  /**
    * The Behavior of the chat room actor
    */
  def behavior: Behavior[RoomCommand] = chatRoom(Map.empty)

  /**
    * Implementation details of the chat room actor
    */
  private def chatRoom(sessions: SessionMap): Behavior[RoomCommand] =
    Actor.immutable[RoomCommand] { (ctx, msg) =>
      msg match {
        case PostMessage(ref, msg) =>
          sessions.get(ref) foreach { name =>
            sessions.foreach(_._1 ! DisplayMessage(s"$name: $msg"))
          }
          Actor.same
        case JoinRoom(newUser, name) if actorWithName(name)(sessions).isEmpty =>
          newUser ! Connected(ctx.self, sessions.values.toSeq)
          val newSessions = sessions + ((newUser, name))
          ctx.watch(newUser)
          newSessions.foreach(_._1 ! DisplayMessage(s"[System] $name has connected."))
          chatRoom(newSessions)
        case JoinRoom(newUser, name) =>
          newUser ! LoginDenied(s"A user with the name $name is already logged in.")
          Actor.same
      }
    } onSignal {
      case (ctx, Terminated(ref)) =>
        val newSessions = sessions.filterNot(_._1 == ref)
        sessions foreach {
          case (r, name) if r == ref =>
            newSessions.foreach(_._1 ! DisplayMessage(s"[System] $name has disconnected."))
          case _ =>
        }
        chatRoom(newSessions)
    }
}

/**
  * Messages that are sent out to a client through a websocket.
  */
sealed trait ClientOutput
final case class Message(message: String) extends ClientOutput
final case class Disconnected(reason: String) extends ClientOutput

/**
  * A message sent from a chat room actor to a client's output handler.
  */
sealed trait ChatEvent
final case class Connected(room: ActorRef[RoomCommand], onlineUsers: Seq[String]) extends ChatEvent
final case class DisplayMessage(message: String) extends ChatEvent
final case class LoginDenied(reason: String) extends ChatEvent

/**
  * Top-level trait for messages that are handled by the client and chat room
  * actors.
  */
sealed trait Command

/**
  * Internal message that is sent by the chat room actor to a client's input
  * handler.
  */
sealed trait InputHandlerCommand extends Command
final case class SwitchToConnected(room: ActorRef[RoomCommand]) extends InputHandlerCommand
final case object ForceDisconnect extends InputHandlerCommand

/**
  * Message that is sent through the websocket and is processed by a client's
  * input handler.
  */
sealed trait ClientCommand extends Command
final case class Connect(name: String) extends ClientCommand
final case class SendMessage(message: String) extends ClientCommand

/**
  * Internal message that is sent by a client actor to the chat room actor.
  */
sealed trait RoomCommand
final case class JoinRoom(handler: ActorRef[ChatEvent], name: String) extends RoomCommand
final case class PostMessage(handler: ActorRef[ChatEvent], message: String) extends RoomCommand
