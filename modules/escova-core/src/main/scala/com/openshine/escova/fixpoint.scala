package com.openshine.escova

import java.time.LocalDateTime
import java.time.temporal.{ChronoField, ChronoUnit, TemporalField, TemporalUnit}
import java.util.Locale

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object fixpoint {

  sealed abstract class DateFixPoint(truncate: TemporalUnit,
                                     field: TemporalField,
                                     validRange: List[Int] = List(1))
      extends DateRange {
    override def toMap: Map[String, String] = Map(
      "type" -> "since",
      "since" -> field.getDisplayName(Locale.ROOT)
    )
    def matches(dt: LocalDateTime): Boolean = {
      if (dt.truncatedTo(truncate)
            .compareTo(
              dt.truncatedTo(ChronoUnit.SECONDS)
            ) == 0) {
        dt.get(field) == 1
      } else false
    }
  }

  case object CurrentDay
      extends DateFixPoint(ChronoUnit.MINUTES, ChronoField.HOUR_OF_DAY)

  case object FirstOfWeek
      extends DateFixPoint(ChronoUnit.MINUTES, ChronoField.DAY_OF_WEEK)

  case object FirstOfMonth
      extends DateFixPoint(ChronoUnit.MINUTES, ChronoField.DAY_OF_MONTH)

  // Cnnot be implemented directly:
  // case object FirstOfQuarter extends DateFixPoint(...)

  case object FirstOfYear
      extends DateFixPoint(ChronoUnit.MINUTES, ChronoField.DAY_OF_YEAR)

  val all = List(
    CurrentDay,
    FirstOfWeek,
    FirstOfMonth,
    FirstOfYear
  )

}
