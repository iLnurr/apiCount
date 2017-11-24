import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {
  val host = "0.0.0.0" // Gets the host and a port
  val port = 5000

  val requestTimeOut = 20.seconds

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher  //bindAndHandle requires an implicit ExecutionContext

  val api = new RestApi(system, requestTimeOut).routes // the RestApi provides a Route

  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) //Starts the HTTP server

  val log =  Logging(system.eventStream, "apiCount")
  bindingFuture onComplete {
    case Success(serverBinding) =>
      log.info(s"RestApi bound to ${serverBinding.localAddress} ")
    case Failure(ex) =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}