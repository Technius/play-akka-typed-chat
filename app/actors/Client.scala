package actors

import akka.typed._
import akka.typed.scaladsl.Actor

object Client {

  /**
    * The default behavior of a client actor. This actor handles all Command
    * messages, since its state is dependent on both the messages from the
    * client and the messages from the chat room.
    *
    * @param connector An actor that attempts to connect the client to the
    * chat room when it receives a JoinRoom message.
    * @param out The actor that writes received messages to the websocket.
    */
  def behavior(connector: ActorRef[JoinRoom], out: ActorRef[ClientOutput]): Behavior[Command] =
    Actor.deferred { ctx =>
      val outHandler = ctx.spawnAnonymous(outputHandler(ctx.self, out))
      unconnectedBehavior(connector, outHandler)
    }

  /**
    * The behavior of a client actor when it is not connected to any chat room.
    */
  private def unconnectedBehavior(
      connector: ActorRef[JoinRoom],
      outHandler: ActorRef[ChatEvent]): Behavior[Command] =
    Actor.immutable[Command] { (_, msg) =>
      msg match {
        case Connect(name) =>
          connector ! JoinRoom(outHandler, name)
          Actor.same
        case ForceDisconnect =>
          Actor.stopped
        case SwitchToConnected(handle) => connectedBehavior(handle, outHandler)
        case _ => Actor.same
      }
    }

  /**
    * The behavior of a client actor when it connected to a chat room.
    */
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

  /**
    * An actor that handles messages from a chat room.
    */
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
        case LoginDenied(reason) =>
          output ! Disconnected(s"Unable to log in (reason: $reason)")
          inputHandler ! ForceDisconnect
        case _ =>
      }
      Actor.same
    } onSignal {
      case (ctx, Terminated(ref)) if ref == output =>
        Actor.stopped
    }
}
