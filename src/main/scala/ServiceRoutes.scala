import akka.actor.ActorSystem
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.directives.CachingDirectives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import models.Statistic

trait ServiceRoutes extends Persistence with CachingDirectives {

  // Set up a separate thread pool for blocking operations
  // We *could* configure this, but the default implementation is probably fine
  implicit override val ec: ExecutionContext =
    ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool)

  implicit val routeCache: Cache[Uri, RouteResult]

  val cacheKeyer: PartialFunction[RequestContext, Uri] = {
    // Only cache GETs
    val isGet: RequestContext => Boolean = _.request.method == GET
    PartialFunction {
      case r: RequestContext if isGet(r) => r.request.uri
    }
  }

  val routes = {
    logRequestResult("analytics") {
      path("analytics") {
        post {
          parameter("timestamp".as[Long], "user", "event") {
            (timeStamp, user, clickOrImpression) =>
              // Fire & Forget
              Future {
                val newStatistic =
                  Statistic(timeStamp, user, clickOrImpression)
                persistStatistic(newStatistic)
              }
              complete(NoContent)
          }
        } ~
          get {
            parameter("timestamp".as[Long]) { (timeStamp) =>
              val (uniqueFuture, clicksFuture, impressionsFuture) =
                getStatistics(timeStamp)
              val aggregate: Future[String] = for {
                unique <- uniqueFuture
                clicks <- clicksFuture
                impressions <- impressionsFuture
              } yield
                s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
              val message = Await.result(aggregate, Duration(2, SECONDS))

              cache(routeCache, cacheKeyer) {
                complete((OK, message))
              }
            }
          }
      }
    }
  }
}
