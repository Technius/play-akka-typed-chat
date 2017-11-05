package controllers

import akka.actor._
import javax.inject._
import julienrf.json.derived
import play.api._
import play.api.mvc._
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import akka.stream.Materializer
import akka.typed.scaladsl.adapter._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents)(
  implicit actorSystem: ActorSystem,
  mat: Materializer
) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  val roomActor = actorSystem.spawn(actors.ChatRoom.chatRoom(Map.empty), "room")

  implicit val clientCommandReads: OFormat[actors.ClientCommand] =
    derived.oformat()

  implicit val flowTransformer: MessageFlowTransformer[actors.ClientCommand, String] =
    MessageFlowTransformer.jsonMessageFlowTransformer[actors.ClientCommand, String]

  def chat() = WebSocket.accept[actors.ClientCommand, String] { request =>
    ActorFlow.actorRef { out =>
      PropsAdapter(actors.ChatRoom.unconnectedClient(roomActor, out))
    }
  }
}
