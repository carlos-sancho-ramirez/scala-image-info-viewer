import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object ImageInfoRestService extends App with ImageRoutes {
  implicit val system: ActorSystem = ActorSystem("imageCheckerRestService")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override val imageCheckerActor: ActorRef = system.actorOf(ImageCheckerActor.props, "imageCheckerActor")

  lazy val routes: Route = imageRoutes

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)
}
