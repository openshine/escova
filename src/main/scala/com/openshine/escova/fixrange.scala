package com.openshine.escova

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object fixrange {

  val all = List(
    Year, Month31, Week, WorkWeek, Day, Hour, Minute, Second
  )

  sealed abstract class DateUnit(val len: Long) extends DateRange

  case class DateUnitMultiple(multiplicity: Int, base: DateUnit)
    extends DateUnit(base.len * multiplicity)

  case object Second extends DateUnit(1000)

  case object Minute extends DateUnit(60000)

  case object Hour extends DateUnit(3600000)

  case object Day extends DateUnit(Hour.len * 24)

  case object WorkWeek extends DateUnit(Day.len * 5)

  case object Week extends DateUnit(Day.len * 7)

  case object Month31 extends DateUnit(Day.len * 31)

  case object Year extends DateUnit(Day.len * 365)
}
