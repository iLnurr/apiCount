import java.io.IOException

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{ExecutionContext, Future}

object Worker {
  def props: Props = Props(new Worker())

  case class Work(workName: String)
  case class WorkResult(r: String)
  case object WorkDone
  case object GetTask
  case class Task(wordToCount: String)
}

class Worker extends Actor {
  import Worker._

  implicit val system = context.system
  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()

  def idle: Receive = {
    case Work(workName) =>
      val master = sender()
      context.become(working(workName, master))
      master ! GetTask
  }

  def working(workName: String, master: ActorRef): Receive = {
    case Task(wordToCount) =>
      process(wordToCount) map (r => master ! WorkResult(r))
      master ! GetTask
    case WorkDone =>
      context.become(finish(sender()))
  }
  def finish(master: ActorRef): Receive = {
    case Terminated(master) =>
      println("finish worker")
      context.stop(self)
  }

  override def receive: Receive = idle

  private def process(wordToCount: String): Future[String] = {
    def apiRequest(request: HttpRequest)(implicit system: ActorSystem, ex: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
      val hostnameApi = "api.stackexchange.com"
      val portApi = 80
      val ipApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(hostnameApi, portApi)
      Source.single(request).via(ipApiConnectionFlow).runWith(Sink.head)
    }

    def getInfo(word: String)(implicit system: ActorSystem, ex: ExecutionContext, mat: Materializer): Future[String] = {
      val linkApi = s"/2.2/search?pagesize=100&order=desc&sort=creation&tagged=${word}&site=stackoverflow"
      apiRequest(RequestBuilding.Get(linkApi)).flatMap { response ⇒
        response.status match {
          case OK ⇒ Unmarshal(response.entity).to[String]
          case _ ⇒ Unmarshal(response.entity).to[String].flatMap { entity ⇒
            val error = s"Request failed with status code ${response.status} and entity $entity"
            Future.failed(new IOException(error))
          }
        }
      }
    }
    getInfo(wordToCount)
  }

}
