import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  implicit val imageDetailsJsonFormat = new RootJsonFormat[ImageDetails] {
    override def read(json: JsValue): ImageDetails = ???

    override def write(obj: ImageDetails): JsValue = {
      JsObject(Map("header" -> JsString("h"), "frame" -> JsString("f")))
    }
  }
}
