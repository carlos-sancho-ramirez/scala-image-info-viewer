import java.io.{FileInputStream, IOException, InputStream}

object ImageInfoDumper {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println("Image path is required")
    }
    else {
      for (imagePath <- args) {
        try {
          implicit val inStream: InputStream = new FileInputStream(imagePath)
          try {
            val result = ImageInfoGetter.getInfo(inStream)
            if (result.nonEmpty) {
              val details = result.get
              import details.header
              import details.frame

              println(s"Image found: $imagePath")
              println(s"  ${header.format}")
              println(s"  Resolution: ${frame.width}x${frame.height}")
              println(s"  Bits per sample: ${frame.bitsPerSample}")

              if (frame.components.nonEmpty) {
                println("  Components:")
                for (component <- frame.components) {
                  println(s"    id: ${component.id}. Sampling: ${component.horizontalSampling}x${component.verticalSampling}. Quantization table id: ${component.quantizationTableId}")
                }
              }

              for (entry <- header.details) {
                println(s"  ${entry._1}: ${entry._2}")
              }
            }
            else {
              System.err.println(s"Invalid image: $imagePath")
            }
          }
          finally {
            try {
              inStream.close()
            }
            catch {
              case _: IOException => // Nothing to be done
            }
          }
        }
        catch {
          case e: IOException => System.err.println(s"Unable to open file $imagePath. ${e.getMessage}")
        }
      }
    }
  }
}
