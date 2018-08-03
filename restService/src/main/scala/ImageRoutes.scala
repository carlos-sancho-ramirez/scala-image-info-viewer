import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

trait ImageRoutes extends JsonSupport {
  implicit def system: ActorSystem
  lazy val log = Logging(system, classOf[ImageRoutes])

  def imageCheckerActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val imageRoutes: Route =
    concat(
      pathPrefix("image") {
        pathEnd {
          post {
            uploadedFile("imageFile") { case (_, file) =>
              val details: Future[ImageDetails] = (imageCheckerActor ? ImageCheckerActor.GetDetails(file)).mapTo[ImageDetails]
              complete(details)
            }
          }
        }
      },
      get {
        getFromResource("index.html")
      }
    )
}
