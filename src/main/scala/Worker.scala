import java.io.IOException

import Worker.{Answer, Owner}
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object Worker {
  def props: Props = Props(new Worker())

  case class Work(workName: String)
  case class WorkResult(r: String)
  case object WorkDone
  case object GetTask
  case class Task(wordToCount: String)
  case class Answer(
                     tags: Option[List[String]],
                     owner: Owner,
                     is_answered: Option[Boolean],
                     view_count: Option[Int],
                     answer_count: Option[Int],
                     score: Option[Int],
                     last_activity_date: Option[Long],
                     creation_date: Option[Long],
                     question_id: Option[Long],
                     link: Option[String],
                     title: Option[String]
                   )
  case class Owner(
                    reputation: Option[Int],
                    user_id: Option[Long],
                    user_type: Option[String],
                    accept_rate: Option[Int],
                    profile_name: Option[String],
                    display_name: Option[String],
                    link: Option[String]
                  )
}

class Worker extends Actor with ActorLogging with HttpClient {
  import Worker._

  implicit val system = context.system
  implicit val ex = context.dispatcher
  implicit val mat = ActorMaterializer()

  def idle: Receive = {
    case Work(workName) =>
      log.debug(s"Got work with name:${workName}")
      val master = sender()
      context.become(working(workName, master))
      context.watch(master)
      master ! GetTask
  }

  def working(workName: String, master: ActorRef): Receive = {
    case Task(wordToCount) =>
      log.debug(s"Got task with word:${wordToCount}")
      process(wordToCount) foreach { r =>
        master ! WorkResult(r)
        master ! GetTask
      }
    case WorkDone =>
      log.debug(s"Got msg workDone")
      context.become(finish(sender()))
  }
  def finish(master: ActorRef): Receive = {
    case Terminated(master) =>
      log.debug("Finish worker")
      context.stop(self)
  }

  override def receive: Receive = idle
}

trait HttpClient extends JsonProtocol {
  implicit val system: ActorSystem
  implicit val ex: ExecutionContext
  implicit val mat: Materializer

  def process(wordToCount: String): Future[String] = {
    getInfo(wordToCount)
  }

  private def getInfo(word: String) = {
    val hostnameApi = "http://api.stackexchange.com"
    val linkApi = s"/2.2/search?pagesize=100&order=desc&sort=creation&tagged=${word}&site=stackoverflow"
    val req = request(s"${hostnameApi}${linkApi}")
    val eventualHttpResponse = Http().singleRequest(req).map(decodeResponse)
    eventualHttpResponse flatMap { response =>
      response.status match {
        case OK ⇒
          Unmarshal(response.entity.withContentType(ContentTypes.`application/json`)).to[String]
        case _ ⇒
          Unmarshal(response.entity).to[String].flatMap { entity ⇒
            val error = s"Request failed with status code ${response.status} and entity $entity"
            Future.failed(new IOException(error))
          }
      }
    }
  }

  private def request(uri: String) = HttpRequest(uri = Uri(uri))

  private def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip ⇒
        Gzip
      case HttpEncodings.deflate ⇒
        Deflate
      case HttpEncodings.identity ⇒
        NoCoding
      case _ =>
        NoCoding

    }

    decoder.decodeMessage(response)
  }
}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat11(Answer)
  implicit val ownerFormat: RootJsonFormat[Owner] = jsonFormat7(Owner)
}
