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

  override def composeHtmlResponse(details: ImageDetails): String = {
    val fileDetails = details.header.details.map { case (k, v) => s"<li>$k: $v</li>" }.mkString
    val components = details.frame.components.map { comp =>
      s"<tr><td>${comp.id}</td><td>${comp.horizontalSampling}x${comp.verticalSampling}</td><td>${comp.quantizationTableId}</td></tr>"
    }.mkString

    s"""
       |<html>
       | <head>Image Details</head>
       | <body>
       |  <h2>File Details</h2>
       |  <ul>
       |   <li>Format: ${details.header.format}</li>
       |   $fileDetails
       |  </ul>
       |  <h2>Picture details</h2>
       |  <ul>
       |   <li>Resolution: ${details.frame.width}x${details.frame.height}</li>
       |   <li>Bits per sample: ${details.frame.bitsPerSample}</li>
       |  </ul>
       |  <table>
       |   <tr>
       |    <td>id</td>
       |    <td>Sampling</td>
       |    <td>Quantization table id</td>
       |   </tr>
       |   $components
       |  </table>
       | </body>
       |</html>
     """.stripMargin
  }

  lazy val routes: Route = imageRoutes

  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
  Await.result(system.whenTerminated, Duration.Inf)
}
