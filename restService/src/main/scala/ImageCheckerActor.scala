import akka.actor.{ Actor, ActorLogging, Props }

object ImageCheckerActor {
  final case object GetDetails

  def props: Props = Props[ImageCheckerActor]
}

class ImageCheckerActor extends Actor with ActorLogging {
  import ImageCheckerActor._

  def receive: Receive = {
    case GetDetails =>
      sender() ! ImageDetails(Header("Mock", Map()), FrameDetails(8, 32, 32, Seq()))
  }
}
