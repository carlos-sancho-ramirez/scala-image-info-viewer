import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  implicit val imageDetailsJsonFormat = new RootJsonFormat[ImageDetails] {
    override def read(json: JsValue): ImageDetails = {
      json match {
        case JsObject(fields) =>
          val headerFields = fields("header").asJsObject.fields
          val format = headerFields("format") match {
            case JsString(value) => value
          }
          val details: Map[String, String] = headerFields.get("details").flatMap(_ match {
            case JsObject(detailsMap) => Some(detailsMap.collect {
              case (key, jsValue: JsString) => (key, jsValue.value)
            })
            case _ => None
          }).getOrElse(Map())
          val header = Header(format, details)

          val frameDetailsFields = fields("frame").asJsObject.fields
          val width = frameDetailsFields("width").asInstanceOf[JsNumber].value.intValue()
          val height = frameDetailsFields("height").asInstanceOf[JsNumber].value.intValue()
          val bitsPerSample = frameDetailsFields("bitsPerSample").asInstanceOf[JsNumber].value.intValue()
          val frameDetails = FrameDetails(bitsPerSample, width, height, Seq())

          ImageDetails(header, frameDetails)
      }
    }

    override def write(obj: ImageDetails): JsValue = {
      val header = JsObject("format" -> JsString(obj.header.format), "details" -> JsObject(obj.header.details.mapValues(v => JsString(v))))
      val frame = JsObject("width" -> JsNumber(obj.frame.width), "height" -> JsNumber(obj.frame.height), "bitsPerSample" -> JsNumber(obj.frame.bitsPerSample))
      JsObject("header" -> header, "frame" -> frame)
    }
  }
}
