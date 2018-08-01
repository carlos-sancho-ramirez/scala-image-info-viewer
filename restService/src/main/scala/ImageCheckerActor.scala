import java.io.{File, FileInputStream}

import akka.actor.{Actor, ActorLogging, Props}

object ImageCheckerActor {
  final case class GetDetails(imageFile: File)

  def props: Props = Props[ImageCheckerActor]
}

class ImageCheckerActor extends Actor with ActorLogging {
  import ImageCheckerActor._

  def receive: Receive = {
    case GetDetails(imageFile) =>
      sender() ! ImageInfoGetter.getInfo(new FileInputStream(imageFile)).get
  }
}
