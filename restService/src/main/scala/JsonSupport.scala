import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  implicit val imageDetailsJsonFormat = new RootJsonFormat[ImageDetails] {
    object FrameComponentDetailsKeys {
      val id = "id"
      val horizontalSampling = "horizontalSampling"
      val verticalSampling = "verticalSampling"
      val quantizationTableId = "quantizationTableId"
    }

    object FrameKeys {
      val width = "width"
      val height = "height"
      val bitsPerSample = "bitsPerSample"
      val components = "components"
    }

    object HeaderKeys {
      val format = "format"
      val details = "details"
    }

    object Keys {
      val header = "header"
      val frame = "frame"
    }

    private def readComponent(json: JsObject): FrameComponentDetails = {
      val id = json.fields(FrameComponentDetailsKeys.id).asInstanceOf[JsNumber].value.intValue()
      val horizontalSampling = json.fields(FrameComponentDetailsKeys.horizontalSampling).asInstanceOf[JsNumber].value.intValue()
      val verticalSampling = json.fields(FrameComponentDetailsKeys.verticalSampling).asInstanceOf[JsNumber].value.intValue()
      val quantizationTableId = json.fields(FrameComponentDetailsKeys.quantizationTableId).asInstanceOf[JsNumber].value.intValue()
      FrameComponentDetails(id, horizontalSampling, verticalSampling, quantizationTableId)
    }

    override def read(json: JsValue): ImageDetails = {
      json match {
        case JsObject(fields) =>
          val headerFields = fields(Keys.header).asJsObject.fields
          val format = headerFields(HeaderKeys.format) match {
            case JsString(value) => value
          }
          val details: Map[String, String] = headerFields.get(HeaderKeys.details).flatMap(_ match {
            case JsObject(detailsMap) => Some(detailsMap.collect {
              case (key, jsValue: JsString) => (key, jsValue.value)
            })
            case _ => None
          }).getOrElse(Map())
          val header = Header(format, details)

          val frameDetailsFields = fields(Keys.frame).asJsObject.fields
          val width = frameDetailsFields(FrameKeys.width).asInstanceOf[JsNumber].value.intValue()
          val height = frameDetailsFields(FrameKeys.height).asInstanceOf[JsNumber].value.intValue()
          val bitsPerSample = frameDetailsFields(FrameKeys.bitsPerSample).asInstanceOf[JsNumber].value.intValue()
          val components = frameDetailsFields.getOrElse(FrameKeys.components, JsArray()).asInstanceOf[JsArray].elements
            .collect { case element: JsObject => readComponent(element) }
          val frameDetails = FrameDetails(bitsPerSample, width, height, components)

          ImageDetails(header, frameDetails)
      }
    }

    private def write(obj: FrameComponentDetails): JsValue = {
      JsObject(
        FrameComponentDetailsKeys.id -> JsNumber(obj.id),
        FrameComponentDetailsKeys.horizontalSampling -> JsNumber(obj.horizontalSampling),
        FrameComponentDetailsKeys.verticalSampling -> JsNumber(obj.verticalSampling),
        FrameComponentDetailsKeys.quantizationTableId -> JsNumber(obj.quantizationTableId)
      )
    }

    override def write(obj: ImageDetails): JsValue = {
      val components = JsArray(obj.frame.components.map(write).toVector)
      val header = JsObject(HeaderKeys.format -> JsString(obj.header.format), HeaderKeys.details -> JsObject(obj.header.details.mapValues(v => JsString(v))))
      val frame = JsObject(
        FrameKeys.width -> JsNumber(obj.frame.width),
        FrameKeys.height -> JsNumber(obj.frame.height),
        FrameKeys.bitsPerSample -> JsNumber(obj.frame.bitsPerSample),
        FrameKeys.components -> components
      )
      JsObject(Keys.header -> header, Keys.frame -> frame)
    }
  }
}
