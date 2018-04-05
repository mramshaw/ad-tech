import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes.{NoContent, OK}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.Statistic
import org.joda.time.{DateTime, DateTimeUtils, DateTimeZone, Instant}
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class AnalyticsServiceSuite
    extends FlatSpec
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with Persistence
    with ServiceRoutes {

  // Change the cache settings in 'application.conf'
  val defaultCachingSettings = CachingSettings(system)

  val routeCache: Cache[Uri, RouteResult] = LfuCache(defaultCachingSettings)

  val formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

  def getMillis(yyyy: Int, mon: Int, dd: Int, hh: Int, mm: Int): Long = {
    val dateTime = new DateTime(yyyy, mon, dd, hh, mm)
    val millis = dateTime.getMillis()
    val ins = new Instant(millis)
    val mdt = ins.toMutableDateTime()
    mdt.setZone(DateTimeZone.UTC)
    val dateStr = formatter.print(mdt);
    println(s"Millis: $millis is $dateStr!")
    millis
  }

  override def beforeAll {

    // Block until we have a database
    Await.result(createSchema(), Duration.Inf)

    // Set the current time to Midnight, New Years Eve
    // val millis = getMillis(2018, 1, 1, 0, 0)
    val millis = 1514764800000L
    // 1514764800000L is 2018-01-01T00:00:00.000Z
    DateTimeUtils.setCurrentMillisFixed(millis);

    // millis per hour = 1,000 * 60 * 60 = 3,600,000

    //  Fred should be pre-NYE while Naomi & Irene should be post-NYE
    var time = millis - 3600000 - 1
    for (i <- 1 to 10) {
      Post(s"/analytics?timestamp=$time&user=Fred&event=click") ~> routes
      time = time + 360000
    }
    time = millis
    for (i <- 1 to 10) {
      Post(s"/analytics?timestamp=$time&user=Naomi&event=impression") ~> routes
      time = time + 360000
    }
    time = millis
    for (i <- 1 to 10) {
      Post(s"/analytics?timestamp=$time&user=Irene&event=click") ~> routes
      time = time + 360000
    }
    time = millis
    for (i <- 1 to 10) {
      Post(s"/analytics?timestamp=$time&user=Irene&event=impression") ~> routes
      time = time + 360000
    }
    for (i <- 1 to 5) {
      Post(s"/analytics?timestamp=$time&user=Irene&event=impression") ~> routes
      time = time + 360000
    }
  }

  it should "return 0 records in response to get Statistics (9-10 PM Pre-New Year)" in {
    // NYE - 2 hours - 1
    val millis = 1514764800000L - 3600000 - 360000 - 1
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 0
      val clicks = 0
      val impressions = 0
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "return 1 record in response to get Statistics (10-11 PM Pre-New Year)" in {
    // NYE - 1 hour - 1
    val millis = 1514764800000L - 3600000 - 1
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 1
      val clicks = 1
      val impressions = 0
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "respond to get Statistics (New Year)" in {
    // should collapse to the exact millisecond of NYE
    val millis = 1514764800000L
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 2
      val clicks = 1
      val impressions = 2
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "respond to get Statistics pre-New Year" in {
    // Subtract 1 from NYE to avoid rounding issues
    val millis = 1514764800000L - 1L
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 1
      val clicks = 9
      val impressions = 0
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "respond to get Statistics (1 AM)" in {
    // Get the time for 1 AM, New Years Eve
    //val millis = getMillis(2018, 1, 1, 1, 0)
    val millis = 1514768400000L - 1L
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 2
      val clicks = 10
      val impressions = 20
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "respond to post Statistic" in {
    val time = 1514768400000L - 1L
    Post(s"/analytics?timestamp=$time&user=Post&event=click") ~> routes ~> check {
      status shouldBe NoContent
      //  It does not seem possible to test for the following:
      //  contentType shouldBe `none/none`
      responseAs[String].length shouldEqual 0
    }
  }

  it should "use cached Statistics (First 1 AM)" in {
    val time = 1514768400000L - 1L
    Post(s"/analytics?timestamp=$time&user=Cache&event=impression") ~> routes
    val millis = 1514768400000L - 1L
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 2
      val clicks = 10
      val impressions = 20
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "NOT use cached Statistics (First)" in {
    val millis = 1514768400000L - 1L
    // Caching prevented
    Get(s"/analytics?timestamp=$millis") ~> `Cache-Control`(`no-cache`) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 4
      val clicks = 11
      val impressions = 21
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "use cached Statistics (Second 1 AM)" in {
    val time = 1514764800000L
    Post(s"/analytics?timestamp=$time&user=NYE&event=impression") ~> routes
    val millis = 1514768400000L - 1L
    Get(s"/analytics?timestamp=$millis") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 2
      val clicks = 10
      val impressions = 20
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  it should "NOT use cached Statistics (Second)" in {
    val millis = 1514768400000L - 1L
    // Caching prevented
    Get(s"/analytics?timestamp=$millis") ~> `Cache-Control`(`no-cache`) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `text/plain(UTF-8)`
      val unique = 5
      val clicks = 11
      val impressions = 22
      responseAs[String] shouldEqual s"unique_users,$unique\nclicks,$clicks\nimpressions,$impressions\n"
    }
  }

  override def afterAll {
    dropSchema()
  }
}
