package com.openshine.escova

import java.time.temporal.ChronoUnit

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object fixrange {
  val all = List(
    Year, Month, Week, Day, Hour, Minute, Second
  )

  sealed abstract class DateUnit(val name: String, val len: ChronoUnit)
    extends DateRange

  case class DateUnitMultiple(multiplicity: Int, base: DateUnit)
    extends DateUnit(base.name, base.len)

  case object Second extends DateUnit("second", ChronoUnit.SECONDS)

  case object Minute extends DateUnit("minute", ChronoUnit.MINUTES)

  case object Hour extends DateUnit("hour", ChronoUnit.HOURS)

  case object Day extends DateUnit("day", ChronoUnit.DAYS)

  case object Week extends DateUnit("week", ChronoUnit.WEEKS)

  case object Month extends DateUnit("month", ChronoUnit.MONTHS)

  case object Year extends DateUnit("year", ChronoUnit.YEARS)
}
