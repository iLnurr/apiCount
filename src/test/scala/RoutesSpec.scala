import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.Specs2RouteTest
import akka.http.scaladsl.server._
import Directives._
import org.specs2.mutable.{Specification, SpecificationLike}

import scala.concurrent.duration._

class RoutesSpec extends Specification with Specs2RouteTest {

  val api = new RestApi(system, 10.minute)
  val route = api.routes

  "The restApi" should {
    "return a result" in {
      // tests:
      Get("/search?tag=scala") ~> route ~> check {
        responseAs[String] === "The tags are scala."
      }

      Get("/search?tag=scala&tag=clojure") ~> route ~> check {
        responseAs[String] === "The tags are clojure, scala."
      }
    }
  }
}
