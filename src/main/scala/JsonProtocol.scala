import Master.Results
import Worker.{Answer, Owner, SOFResult}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val ownerFormat: RootJsonFormat[Owner] = jsonFormat8(Owner)
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat11(Answer)
  implicit val sofresultFormat: RootJsonFormat[SOFResult] = jsonFormat4(SOFResult)
  implicit val resultsFormat: RootJsonFormat[Results] = jsonFormat1(Results)
}