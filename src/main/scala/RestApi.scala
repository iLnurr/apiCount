import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, NotFound, OK}
import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, get, onSuccess, pathEndOrSingleSlash, pathPrefix, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
  extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit def executionContext = system.dispatcher

  override def createMaster(): ActorRef = system.actorOf(Master.props())
}

trait RestRoutes extends MasterApi
  with JsonProtocol {

  def routes: Route = pathPrefix("search") {
    tagsRoute
  }

  def tagsRoute = parameters('tag.*) { (tags) =>
      tags.toList match {
        case Nil         => complete(s"There are no tags.")
        case multiple    => complete(ToResponseMarshallable(getCount(multiple)))
      }
    }

}

trait MasterApi {
  import Master._

  def createMaster(): ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  def master = createMaster()

  def getCount(tags: List[String]) =
    master.ask(Start(s"search - $tags", tags))
    .mapTo[Results]

}
