import Master.Results
import Worker.{Answer, Owner, SOFResult, WorkResult}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsNumber, JsObject, JsValue, RootJsonFormat}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val ownerFormat: RootJsonFormat[Owner] = jsonFormat8(Owner)
  implicit val answerFormat: RootJsonFormat[Answer] = jsonFormat11(Answer)
  implicit val sofresultFormat: RootJsonFormat[SOFResult] = jsonFormat4(SOFResult)

  implicit object wrCustomFormat extends RootJsonFormat[WorkResult] {
    override def read(json: JsValue): WorkResult = throw new UnsupportedOperationException("WorkResult class don't support read from json")

    override def write(wr: WorkResult): JsValue = JsObject{
      wr.word -> JsObject(
        "total" -> JsNumber(wr.result.items.size),
        "answered" -> JsNumber(wr.result.items.count(_.is_answered.contains(true)))
      )
    }

  }

  implicit val resultsFormat: RootJsonFormat[Results] = jsonFormat1(Results)
}