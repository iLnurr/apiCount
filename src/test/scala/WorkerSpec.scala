import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.mutable.SpecificationLike
import scala.concurrent.duration._

class WorkerSpec extends TestKit(ActorSystem("worker")) with ImplicitSender with SpecificationLike {
  "Worker" should {
    "correctly procees incoming job" in {
      import Worker._

      val d = 20.second

      val worker = system.actorOf(Worker.props)

      worker ! Work("findWordCount")
      expectMsg(d, GetTask)

      worker ! Task("clojure")
      val result = expectMsgType[Result](d)
      println(result)

      success
    }
  }

}
