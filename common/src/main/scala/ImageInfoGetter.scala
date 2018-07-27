import java.io.{IOException, InputStream}

case class Header(format: String, details: Map[String, String])
case class FrameComponentDetails(id: Int, horizontalSampling: Int, verticalSampling: Int, quantizationTableId: Int)
case class FrameDetails(bitsPerSample: Int, width: Int, height: Int, components: Seq[FrameComponentDetails]) {
  if (bitsPerSample <= 0 || width <= 0 || height <= 0 || components == null) {
    throw new IllegalArgumentException()
  }
}

object ImageInfoGetter {
  private def readUnsignedByte(array: IndexedSeq[Byte], index: Int): Int = {
    if (index >= array.size) -1
    else array(index) & 0xFF
  }

  private def readUnsignedBigEndianShort(s: InputStream): Int = {
    val a = s.read()
    val b = s.read()

    if (a >= 0 && a <= 0xFF && b >= 0 && b <= 0xFF) {
      a * 256 + b
    }
    else {
      -1
    }
  }

  private def readUnsignedBigEndianShort(array: IndexedSeq[Byte], index: Int): Int = {
    if (index + 1 >= array.size) {
      -1
    } else {
      val a = array(index) & 0xFF
      val b = array(index + 1) & 0xFF

      a * 256 + b
    }
  }

  private def readUnsignedLittleEndianShort(array: IndexedSeq[Byte], index: Int): Int = {
    if (index + 1 >= array.size) {
      -1
    } else {
      val a = array(index) & 0xFF
      val b = array(index + 1) & 0xFF

      b * 256 + a
    }
  }

  private def readBigEndianInt(array: IndexedSeq[Byte], index: Int): Int = {
    if (index + 3 >= array.size) {
      -1
    } else {
      val a = array(index) & 0xFF
      val b = array(index + 1) & 0xFF
      val c = array(index + 2) & 0xFF
      val d = array(index + 3) & 0xFF

      (a << 24) | (b << 16) | (c << 8) | d
    }
  }

  private def readLittleEndianInt(array: IndexedSeq[Byte], index: Int): Int = {
    if (index + 3 >= array.size) {
      -1
    } else {
      val a = array(index) & 0xFF
      val b = array(index + 1) & 0xFF
      val c = array(index + 2) & 0xFF
      val d = array(index + 3) & 0xFF

      (d << 24) | (c << 16) | (b << 8) | a
    }
  }

  private def ensureRead(text: String)(implicit s: InputStream): Boolean = !text.getBytes.exists(_ != s.read())

  private def readMarkerContent(s: InputStream): Array[Byte] = {
    val length = readUnsignedBigEndianShort(s) - 2
    val buffer = Array.ofDim[Byte](length)
    val count = s.read(buffer)
    if (count != length) {
      throw new IOException()
    }

    buffer
  }

  object JpegMarkers {
    val SOI = 0xFFD8 // Start of Image
    val APP0 = 0xFFE0 // Usually including JFIF (JPEG File Interchange Format)
    val APP1 = 0xFFE1 // Usually including Exif
    val SOF = 0xFFC0 // Start of Frame
    val SOS = 0xFFDA // Start of Scan
  }

  object TiffEndianisms {
    val BIG = 0x4D4D
    val LITTLE = 0x4949
  }

  object DataTypes {
    val BYTE = 1
    val ASCII = 2
    val WORD = 3
    val DWORD = 4
    val RATIONAL = 5
  }

  private def processJfifHeader(content: Array[Byte]): Option[Header] = {
    if (content.length >= 7 && content.zip("JFIF\0".getBytes()).forall(i => i._1 == i._2)) {
      val majorVersion = content(5)
      val minorVersion = content(6)
      Some(Header(s"JFIF version $majorVersion.$minorVersion", Map()))
    }
    else None
  }

  val tiffIfdProperties: Map[Int, String] = Map(
    0x010E -> "ImageDescription",
    0x010F -> "Make",
    0x0110 -> "Model",
    0x0112 -> "Orientation",
    0x011A -> "XResolution",
    0x011B -> "YResolution",
    0x0128 -> "ResolutionUnit",
    0x0131 -> "Software",
    0x0132 -> "DateTime",
    0x013E -> "WhitePoint",
    0x013F -> "PrimaryChromaticities"
  )

  private def processExifHeader(content: Array[Byte]): Option[Header] = {
    val endianism = readUnsignedBigEndianShort(content, 6)
    if (content.length >= 8 && content.zip("Exif\0\0".getBytes()).forall(i => i._1 == i._2) &&
      (endianism == TiffEndianisms.BIG || endianism == TiffEndianisms.LITTLE)) {
      val readUnsignedShort: (Array[Byte], Int) => Int = {
        if (endianism == TiffEndianisms.LITTLE) readUnsignedLittleEndianShort(_,_)
        else readUnsignedBigEndianShort(_,_)
      }
      val readInt: (Array[Byte], Int) => Int = {
        if (endianism == TiffEndianisms.LITTLE) readLittleEndianInt(_,_)
        else readBigEndianInt(_,_)
      }

      val tiffMagic = readUnsignedShort(content, 8)
      val firstIfdOffset = readInt(content, 10) + 6
      val ifdEntries = if (firstIfdOffset >= 14) readUnsignedShort(content, firstIfdOffset) else 0
      if (tiffMagic == 0x002A && firstIfdOffset >= 14 && ifdEntries >= 0) {
        val details = scala.collection.mutable.Map[String,String]()
        for {
          i <- 0 until ifdEntries
          propertyName <- tiffIfdProperties.get(readUnsignedShort(content, firstIfdOffset + 2 + i * 12))
        } {
          val dataType = readUnsignedShort(content, firstIfdOffset + 4 + i * 12)
          val dataLength = readInt(content, firstIfdOffset + 6 + i * 12)
          val dataOffset = readInt(content, firstIfdOffset + 10 + i * 12) + 6
          details(propertyName) = dataType match {
            case DataTypes.ASCII =>
              new String(content, dataOffset, dataLength)
            case DataTypes.WORD =>
              s"${readUnsignedShort(content, firstIfdOffset + 10 + i * 12)}"
            case DataTypes.RATIONAL =>
              if (dataLength != 1) {
                throw new AssertionError()
              }
              val numerator = readInt(content, dataOffset)
              val denominator = readInt(content, dataOffset + 4)
              val rational = numerator.toFloat / denominator
              s"$rational"
            case _ =>
              s"Found with type $dataType and length $dataLength"
          }
        }
        Some(Header("Exif", details.toMap))
      }
      else None
    }
    else None
  }

  private def processHeader(markerId: Int, markerContent: Array[Byte]): Option[Header] = {
    markerId match {
      case JpegMarkers.APP0 => processJfifHeader(markerContent) // So far, assuming that APP0 is always JFIF
      case JpegMarkers.APP1 => processExifHeader(markerContent) // So far, assuming that APP1 is always Exif
      case _ => None
    }
  }

  private def processStartOfFrame(content: Array[Byte]): Option[FrameDetails] = {
    val bitsPerSample = readUnsignedByte(content, 0)
    val width = readUnsignedBigEndianShort(content, 3)
    val height = readUnsignedBigEndianShort(content, 1)

    val componentCount = readUnsignedByte(content, 5)
    val components = for (i <- 0 until componentCount) yield {
      val id = readUnsignedByte(content, 6 + i * 3)
      val samplings = readUnsignedByte(content, 7 + i * 3)
      val quantizationTableId = readUnsignedByte(content, 8 + i * 3)

      val hSampling = (samplings >> 4) & 0x0F
      val vSampling = samplings & 0x0F

      FrameComponentDetails(id, hSampling, vSampling, quantizationTableId)
    }

    if (bitsPerSample > 0 && width > 0 && height > 0) Some(FrameDetails(bitsPerSample, width, height, components))
    else None
  }

  def getInfo(inStream: InputStream): Option[(Header, FrameDetails)] = {
    if (readUnsignedBigEndianShort(inStream) == JpegMarkers.SOI) {
      val firstMarkerId = readUnsignedBigEndianShort(inStream)
      val firstMarkerContent = readMarkerContent(inStream)
      val headerOpt = processHeader(firstMarkerId, firstMarkerContent)

      var frameOpt: Option[FrameDetails] = None
      var markerId: Int = readUnsignedBigEndianShort(inStream)
      while (markerId != JpegMarkers.SOS) {
        val content = readMarkerContent(inStream)
        if (markerId == JpegMarkers.SOF) {
          if (frameOpt.nonEmpty) {
            throw new IOException("Multiple frames found within the file")
          }

          frameOpt = processStartOfFrame(content)
        }

        markerId = readUnsignedBigEndianShort(inStream)
      }

      if (headerOpt.nonEmpty && frameOpt.nonEmpty) Some((headerOpt.get, frameOpt.get))
      else None
    }
    else None
  }
}
