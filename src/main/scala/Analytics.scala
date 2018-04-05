import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.caching.scaladsl.Cache
import akka.http.caching.scaladsl.CachingSettings
import akka.http.caching.LfuCache
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.RouteResult
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import scala.io.StdIn
import scala.util.{Failure, Success}

object Analytics extends App with Persistence with ServiceRoutes {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val logger = Logging(system, getClass)

  // Change the cache settings in 'application.conf'
  val defaultCachingSettings = CachingSettings(system)

  val routeCache: Cache[Uri, RouteResult] = LfuCache(defaultCachingSettings)

  val dbFuture = createSchema()
  dbFuture.onComplete {
    case Success(_) => {}
    case Failure(ex) => ex.printStackTrace
  }

  val bindingFuture = Http().bindAndHandle(routes, host, port)

  println(s"Server online at http://$host:$port\nPress ENTER to stop...")
  val in = StdIn.readLine() // let it run until user presses enter

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
