package models

final case class Statistic(timeStamp: Long,
                           userName: String,
                           clickOrImpression: String,
                           id: Option[Long] = None)
