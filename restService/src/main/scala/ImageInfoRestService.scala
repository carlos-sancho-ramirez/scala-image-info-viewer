import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object ImageInfoRestService extends App with ImageRoutes {
  override implicit val system: ActorSystem = ActorSystem("imageCheckerRestService")
  override implicit val executor = system.dispatcher
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override val imageCheckerActor: ActorRef = system.actorOf(ImageCheckerActor.props, "imageCheckerActor")

  lazy val jsonApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection("localhost", 8080)

  override def apiRequest(request: HttpRequest) = Source.single(request).via(jsonApiConnectionFlow).runWith(Sink.head)

  lazy val routes: Route = imageRoutes

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)
}
