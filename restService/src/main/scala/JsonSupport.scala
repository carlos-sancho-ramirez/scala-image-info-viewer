import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  implicit val imageDetailsJsonFormat = new RootJsonFormat[ImageDetails] {
    override def read(json: JsValue): ImageDetails = ???

    override def write(obj: ImageDetails): JsValue = {
      val header = JsObject("format" -> JsString(obj.header.format), "details" -> JsObject(obj.header.details.mapValues(v => JsString(v))))
      val frame = JsObject("width" -> JsNumber(obj.frame.width), "height" -> JsNumber(obj.frame.height), "bitsPerSample" -> JsNumber(obj.frame.bitsPerSample))
      JsObject("header" -> header, "frame" -> frame)
    }
  }
}
