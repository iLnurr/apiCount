import akka.actor._

import scala.collection.mutable.ListBuffer

object Master {
  case class Start(jobName: String, text: List[String])
  case object ProcessResults
  case class Results(r: List[String])
}
class Master extends Actor{
  import Master._
  import Worker._

  var words = ListBuffer.empty[String]
  var results = ListBuffer.empty[String]
  val workers = ListBuffer.empty[ActorRef]

  override def receive: Receive = idle
  def idle: Receive = {
    case Start(jobName, text) =>
      val receiver = sender()
      words ++= text
      context.become(working(jobName, receiver))
      text.foreach(_ => startWorker)
  }
  def working(jobName: String, receiver: ActorRef): Receive = {
    case GetTask =>
      val workerRef = sender()
      if (words.isEmpty){
        workerRef ! WorkDone
      } else {
        val word = words.head
        workerRef ! Task(word)
        val tail = words.tail
        words = tail
      }
    case WorkResult(r) =>
      results += r
      if (results.size == words.size) context.become(finishing(jobName))
      self ! ProcessResults
  }
  def finishing(jobName: String): Receive = {
    case ProcessResults =>
      println(results)
      workers.foreach(_ ! PoisonPill)
  }
  private def startWorker = {
    val worker = context.actorOf(Worker.props)
    workers += worker
    worker ! Work(s"Process word")
  }
}
