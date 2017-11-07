package actors

import akka.typed._
import akka.typed.scaladsl.Actor

object ChatRoom {
  def unconnectedClient(connector: ActorRef[JoinRoom], out: ActorRef[ClientOutput]): Behavior[Command] =
    Actor.deferred { ctx =>
      val outHandler = ctx.spawnAnonymous(outputHandler(ctx.self, out))
      Actor.immutable[Command] { (_, msg) =>
        msg match {
          case Connect(name) =>
            connector ! JoinRoom(outHandler, name)
            Actor.same
          case SwitchToConnected(handle) => connectedClient(handle, outHandler)
          case _ => Actor.same
        }
      } onSignal {
        case (ctx, Terminated(ref)) if ref == out =>
          Actor.stopped
      }
    }

  def connectedClient(room: ActorRef[RoomCommand], outputHandler: ActorRef[ChatEvent]): Behavior[Command] =
    Actor.immutable[Command] { (ctx, command) =>
      command match {
        case SendMessage(msg) =>
          room ! PostMessage(outputHandler, msg)
          Actor.same
        case _ => Actor.same
      }
    }

  def outputHandler(inputHandler: ActorRef[InputHandlerCommand], output: ActorRef[ClientOutput]): Behavior[ChatEvent] =
    Actor.immutable[ChatEvent] { (_, msg) =>
      msg match {
        case DisplayMessage(msg) => output ! Message(msg)
        case Connected(room) =>
          output ! Message("Connected to room")
          inputHandler ! SwitchToConnected(room)
        case _ =>
      }
      Actor.same
    }

  def chatRoom(sessions: Map[ActorRef[ChatEvent], String]): Behavior[RoomCommand] = {
    def name(target: ActorRef[ChatEvent]): Option[String] =
      sessions.collectFirst { case (r, n) if target == r => n }

    Actor.immutable[RoomCommand] { (ctx, msg) =>
      msg match {
        case PostMessage(ref, msg) =>
          sessions.get(ref) foreach { name =>
            sessions.foreach(_._1 ! DisplayMessage(s"$name: $msg"))
          }
          Actor.same
        case JoinRoom(newUser, name) =>
          newUser ! Connected(ctx.self)
          val newSessions = sessions + ((newUser, name))
          ctx.watch(newUser)
          newSessions.foreach(_._1 ! DisplayMessage(s"[System] $name has connected."))
          chatRoom(newSessions)
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

sealed trait ChatEvent
final case class Connected(room: ActorRef[RoomCommand]) extends ChatEvent
final case class DisplayMessage(message: String) extends ChatEvent

sealed trait Command

sealed trait InputHandlerCommand extends Command
final case class SwitchToConnected(room: ActorRef[RoomCommand]) extends InputHandlerCommand

sealed trait ClientCommand extends Command
final case class Connect(name: String) extends ClientCommand
final case class SendMessage(message: String) extends ClientCommand

sealed trait RoomCommand
final case class JoinRoom(handler: ActorRef[ChatEvent], name: String) extends RoomCommand
final case class PostMessage(handler: ActorRef[ChatEvent], message: String) extends RoomCommand
