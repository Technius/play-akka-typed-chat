package actors

import akka.typed._
import akka.typed.scaladsl.Actor

object ChatRoom {
  def chatRoom(sessions: Map[ActorRef[ChatEvent], String]): Behavior[RoomCommand] = {
    def nameOf(target: ActorRef[ChatEvent]): Option[String] =
      sessions.collectFirst { case (r, n) if target == r => n }

    def actorWithName(name: String): Option[ActorRef[ChatEvent]] =
      sessions.collectFirst { case (r, n) if n == name => r }

    Actor.immutable[RoomCommand] { (ctx, msg) =>
      msg match {
        case PostMessage(ref, msg) =>
          sessions.get(ref) foreach { name =>
            sessions.foreach(_._1 ! DisplayMessage(s"$name: $msg"))
          }
          Actor.same
        case JoinRoom(newUser, name) if actorWithName(name).isEmpty =>
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

  def echo(out: ActorRef[String]): Behavior[String] = Actor.immutable { (ctx, msg) =>
    out ! msg
    Actor.same
  }
}

sealed trait ClientOutput
final case class Message(message: String) extends ClientOutput
final case class Disconnected(reason: String) extends ClientOutput

sealed trait ChatEvent
final case class Connected(room: ActorRef[RoomCommand], onlineUsers: Seq[String]) extends ChatEvent
final case class DisplayMessage(message: String) extends ChatEvent
final case class LoginDenied(reason: String) extends ChatEvent

sealed trait Command

sealed trait InputHandlerCommand extends Command
final case class SwitchToConnected(room: ActorRef[RoomCommand]) extends InputHandlerCommand
final case object ForceDisconnect extends InputHandlerCommand

sealed trait ClientCommand extends Command
final case class Connect(name: String) extends ClientCommand
final case class SendMessage(message: String) extends ClientCommand

sealed trait RoomCommand
final case class JoinRoom(handler: ActorRef[ChatEvent], name: String) extends RoomCommand
final case class PostMessage(handler: ActorRef[ChatEvent], message: String) extends RoomCommand
