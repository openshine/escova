package com.openshine.escova

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
trait DateRange {
  def toMap: Map[String, String]
}

case class DateRangeMultiple(multiplicity: Int, base: DateRange)
  extends DateRange {
  override def toMap: Map[String, String] =
    base.toMap ++ Map("type" -> "range", "amount" -> multiplicity.toString)
}
