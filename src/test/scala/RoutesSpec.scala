import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class RoutesSpec extends Specification with Specs2RouteTest {

  val api = new RestApi(system, 10.minute)
  val route = api.routes

  "The restApi" should {
    "return a result" in {
      // tests:
      Get("/search") ~> route ~> check {
        responseAs[String] === "There are no tags."
      }
    }
  }
}
