import java.io.IOException

import Worker.SOFResult
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Worker {
  def props: Props = Props(new Worker())

  case class Work(workName: String)
  case class WorkResult(word: String, result: SOFResult)
  case object WorkDone
  case object GetTask
  case class Task(wordToCount: String)
  case class SOFResult(
                        items: List[Answer] = List(),
                        has_more: Option[Boolean] = None,
                        quota_max: Option[Int] = None,
                        quota_remaining: Option[Int] = None
                      )
  case class Answer(
                     tags: List[String] = List(),
                     owner: Option[Owner] = None,
                     is_answered: Option[Boolean] = None,
                     view_count: Option[Int] = None,
                     answer_count: Option[Int] = None,
                     score: Option[Int] = None,
                     last_activity_date: Option[Long] = None,
                     creation_date: Option[Long] = None,
                     question_id: Option[Long] = None,
                     link: Option[String] = None,
                     title: Option[String] = None
                   )
  case class Owner(
                    reputation: Option[Int] = None,
                    user_id: Option[Long] = None,
                    user_type: Option[String] = None,
                    accept_rate: Option[Int] = None,
                    profile_name: Option[String] = None,
                    profile_image: Option[String] = None,
                    display_name: Option[String] = None,
                    link: Option[String] = None
                  )
}

class Worker extends Actor with ActorLogging with HttpClient {
  import Worker._

  implicit val system = context.system
  implicit val ec = context.dispatcher
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
      process(wordToCount) onComplete {
        case Success(r) =>
          master ! WorkResult(wordToCount, r)
          master ! GetTask
        case Failure(ex) =>
          log.error(ex, ex.getMessage)
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
  implicit val ec: ExecutionContext
  implicit val mat: Materializer

  def process(wordToCount: String): Future[SOFResult] = {
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
          Unmarshal(response.entity).to[SOFResult]
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
