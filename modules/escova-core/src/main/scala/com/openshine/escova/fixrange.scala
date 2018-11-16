package com.openshine.escova

import java.time.{Month, Year}
import java.time.temporal.ChronoUnit

import org.elasticsearch.common.xcontent.XContent

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object fixrange {
  val all = List(
    Year,
    Month,
    Week,
    Day,
    Hour,
    Minute,
    Second
  )

  sealed abstract class DateUnit(val name: String, val len: ChronoUnit)
      extends DateRange {
    def toMap: Map[String, String] = {
      Map(
        "type" -> "unit",
        "unit" -> name,
        "amount" -> "1"
      )
    }
  }

  case class DateUnitMultiple(multiplicity: Int, base: DateUnit)
      extends DateUnit(base.name, base.len) {
    override def toMap: Map[String, String] =
      super.toMap ++ Map("amount" -> multiplicity.toString)
  }

  case object Second extends DateUnit("second", ChronoUnit.SECONDS)

  case object Minute extends DateUnit("minute", ChronoUnit.MINUTES)

  case object Hour extends DateUnit("hour", ChronoUnit.HOURS)

  case object Day extends DateUnit("day", ChronoUnit.DAYS)

  case object Week extends DateUnit("week", ChronoUnit.WEEKS)

  case object Month extends DateUnit("month", ChronoUnit.MONTHS)

  case object Year extends DateUnit("year", ChronoUnit.YEARS)
}
