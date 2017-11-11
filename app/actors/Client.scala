package actors

import akka.typed._
import akka.typed.scaladsl.Actor

object Client {
  def behavior(connector: ActorRef[JoinRoom], out: ActorRef[ClientOutput]): Behavior[Command] =
    Actor.deferred { ctx =>
      val outHandler = ctx.spawnAnonymous(outputHandler(ctx.self, out))
      unconnectedBehavior(connector, outHandler)
    }

  private def unconnectedBehavior(
      connector: ActorRef[JoinRoom],
      outHandler: ActorRef[ChatEvent]): Behavior[Command] =
    Actor.immutable[Command] { (_, msg) =>
      msg match {
        case Connect(name) =>
          connector ! JoinRoom(outHandler, name)
          Actor.same
        case SwitchToConnected(handle) => connectedBehavior(handle, outHandler)
        case _ => Actor.same
      }
    }

  private def connectedBehavior(
      room: ActorRef[RoomCommand],
      outputHandler: ActorRef[ChatEvent]): Behavior[Command] =
    Actor.immutable[Command] { (ctx, command) =>
      command match {
        case SendMessage(msg) =>
          room ! PostMessage(outputHandler, msg)
          Actor.same
        case _ => Actor.same
      }
    }

  private def outputHandler(
      inputHandler: ActorRef[InputHandlerCommand],
      output: ActorRef[ClientOutput]): Behavior[ChatEvent] =
    Actor.immutable[ChatEvent] { (_, msg) =>
      msg match {
        case DisplayMessage(msg) => output ! Message(msg)
        case Connected(room, onlineUsers) =>
          output ! Message("Connected to room")
          if (onlineUsers.isEmpty) {
            output ! Message("You are currently the only user online.")
          } else {
            output ! Message("Currently online: " + onlineUsers.mkString(", "))
          }
          inputHandler ! SwitchToConnected(room)
        case _ =>
      }
      Actor.same
    } onSignal {
      case (ctx, Terminated(ref)) if ref == output =>
        Actor.stopped
    }
}
