import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.mutable.SpecificationLike

class WorkerSpec extends TestKit(ActorSystem("worker")) with ImplicitSender with SpecificationLike {
  "Worker" should {
    "correctly procees incoming job" in {
      import Worker._
      val worker = system.actorOf(Worker.props)

      worker ! Work("findWordCount")
      expectMsg(GetTask)

      worker ! Task("odersky")
      val result = expectMsgType[Result]
      println(result)

      success
    }
  }

}
