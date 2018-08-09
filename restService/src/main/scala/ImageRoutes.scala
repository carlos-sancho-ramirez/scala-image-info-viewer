import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model._

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

trait ImageRoutes extends JsonSupport {
  implicit def system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit def materializer: ActorMaterializer
  lazy val log = Logging(system, classOf[ImageRoutes])

  def imageCheckerActor: ActorRef
  def apiRequest(request: HttpRequest): Future[HttpResponse]
  def composeHtmlResponse(details: ImageDetails): String

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val imageRoutes: Route = {
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
      pathSingleSlash {
        concat(
          post {
            extractRequestEntity { reqEntity =>
              val newRequest = RequestBuilding.Post("/image", reqEntity)
              val futureResponse = apiRequest(newRequest)
              val f = futureResponse.flatMap { response =>
                response.status match {
                  case StatusCodes.OK =>
                    val r = Unmarshal(response.entity).to[ImageDetails].map(composeHtmlResponse)
                    r.map(str => HttpEntity(ContentTypes.`text/html(UTF-8)`, str))
                  case _ => Future.failed(new RuntimeException(s"Invalid status code ${response.status}"))
                }
              }
              complete(f)
            }
          },
          get {
            getFromResource("index.html")
          }
        )
      }
    )
  }
}
