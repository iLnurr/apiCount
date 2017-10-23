import akka.actor._

object Worker {
  def props: Props = Props(new Worker())

  case class Work(workName: String)
  case class Result(r: String)
  case object WorkDone
  case object GetTask
  case class Task(wordToCount: String)
}

class Worker extends Actor {
  import Worker._
  def idle: Receive = {
    case Work(workName) =>
      val master = sender()
      context.become(working(workName, master))
      master ! GetTask
  }

  def working(workName: String, master: ActorRef): Receive = {
    case Task(wordToCount) =>
      master ! Result(process(wordToCount))
    case WorkDone =>
      context.become(finish(sender()))
  }
  def finish(master: ActorRef): Receive = {
    case Terminated(master) =>
      println("finish worker")
      context.stop(self)
  }

  override def receive: Receive = idle

  private def process(wordToCount: String): String = s"$wordToCount : 15"

}
