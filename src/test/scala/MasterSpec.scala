import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.mutable.SpecificationLike
import scala.concurrent.duration._

class MasterSpec extends TestKit(ActorSystem("worker")) with ImplicitSender with SpecificationLike {
  "Master" should {
    "correctly process words" in {
      import Master._

      val d = 20.second

      val master = system.actorOf(Master.props())

      master ! Start("find please word count", List("clojure", "scala", "odersky", "java", "spark"))
      val result = expectMsgType[Results](d)
      println(result.results.size)
      success
    }
  }

}
