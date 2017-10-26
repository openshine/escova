package com.openshine.escova

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
trait DateRange

case class DateRangeMultiple(multiplicity: Int, base: DateRange) extends
  DateRange
