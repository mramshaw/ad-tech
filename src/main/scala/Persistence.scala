import models.Statistic
import org.joda.time.{DateTime, DateTimeZone, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import slick.jdbc.H2Profile.api._

trait Persistence {

  // Set up a separate thread pool for blocking operations
  // We *could* configure this, but the default implementation is probably fine
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool)

  lazy val db = Database.forConfig("h2memDB")

  def createSchema() =
    db.run(statistics.schema.create)

  def dropSchema() =
    db.run(statistics.schema.drop)

  val statistics = TableQuery[Statistics]

  def getStatistics(
      timeStamp: Long): (Future[Int], Future[Int], Future[Int]) = {

    val previousHourMillis = getPreviousHourMillis(timeStamp)

    getStatistics(previousHourMillis, timeStamp)
  }

  def getStatistics(start: Long,
                    end: Long): (Future[Int], Future[Int], Future[Int]) = {

    // This should probably be re-written to re-use the main select as an inner, and then compile them all

    val usersQuery =
      sql"select count(distinct USER_NAME) from STATISTICS where (TIME_STAMP >= $start) and (TIME_STAMP <= $end)"
        .as[Int]
    val usersRS = db.run(usersQuery.head)

    val clicksQuery =
      sql"select count(CLICK_OR_IMPRESSION) from STATISTICS where (CLICK_OR_IMPRESSION = 'click') and (TIME_STAMP >= $start) and (TIME_STAMP <= $end)"
        .as[Int]
    val clicksRS = db.run(clicksQuery.head)

    val impressionsQuery =
      sql"select count(CLICK_OR_IMPRESSION) from STATISTICS where (CLICK_OR_IMPRESSION = 'impression') and (TIME_STAMP >= $start) and (TIME_STAMP <= $end)"
        .as[Int]
    val impressionsRS = db.run(impressionsQuery.head)

    (usersRS.mapTo[Int], clicksRS.mapTo[Int], impressionsRS.mapTo[Int])
  }

  def getPreviousHourMillis(timeStamp: Long): Long = {

    // This should probably be re-written to use java.time.Instant

    // calculate previous hour in millis
    val instant = new Instant(timeStamp)
    val mdt = instant.toMutableDateTime()
    mdt.setZone(DateTimeZone.UTC)
    //println(s"Time supplied: $timeStamp - Date: $mdt")
    mdt.setMinuteOfHour(0)
    mdt.setSecondOfMinute(0)
    mdt.setMillisOfSecond(0)
    val previousHour = mdt.getMillis()
    //println(s"Previous Hour: $previousHour - Date: $mdt")
    previousHour
  }

  def persistStatistic(statistic: Statistic) =
    db.run(statistics += statistic) map { _ =>
      statistic
    }
}

class Statistics(tag: Tag) extends Table[Statistic](tag, "STATISTICS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def timeStamp = column[Long]("TIME_STAMP")
  def userName = column[String]("USER_NAME")
  def clickOrImpression = column[String]("CLICK_OR_IMPRESSION")

  def * =
    (timeStamp, userName, clickOrImpression, id.?) <> (Statistic.tupled, Statistic.unapply)
}

object Statistics {
  val statistics = TableQuery[Statistics]
}
