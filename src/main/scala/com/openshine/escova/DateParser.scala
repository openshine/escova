package com.openshine.escova

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import java.util.function.LongSupplier
import java.util.{Date, Locale}

import com.openshine.escova.fixpoint.DateFixPoint
import com.openshine.escova.fixrange._
import com.openshine.escova.functional.FieldLens
import org.elasticsearch.common.joda.{DateMathParser, FormatDateTimeFormatter, Joda}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, RangeQueryBuilder}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.joda.time.DateTimeZone

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object DateParser {
  type ConvertibleToTime = String
  type FLTuple = (
    FieldLens[QueryBuilder, ConvertibleToTime],
      FieldLens[QueryBuilder, ConvertibleToTime]
    )
  val DEFAULT_DATE_TIME_FORMATTER: FormatDateTimeFormatter =
    Joda.forPattern("strict_date_optional_time||epoch_millis", Locale.ROOT)
  val dateParser = new DateMathParser(DEFAULT_DATE_TIME_FORMATTER)

  def analyze(n: SearchSourceBuilder, fieldName: String): Seq[DateRange] =
    analyze(n, fieldName, DateParser.now)

  def analyze(n: SearchSourceBuilder, fieldName: String,
              nowProvider: LongSupplier): Seq[DateRange] = {
    implicit val _np: LongSupplier = nowProvider
    val query = Option(n).flatMap { n => Option(n.query()) }
    val aggs = Option(n).flatMap { n => Option(n.aggregations()) }


    val possibleTimes: Seq[FLTuple] =
      query.map(findQueryTimes(fieldName)).getOrElse(Seq())

    possibleTimes.flatMap(parseDate)

  }


  def findQueryTimes(fieldName: String): QueryBuilder => Seq[FLTuple] = {
    case query: RangeQueryBuilder =>
      if (query.fieldName() == fieldName) {
        Seq(
          (FieldLens(query)(_.from().toString, _.from), FieldLens(query)(_.to()
            .toString, _.to)))
      } else {
        Seq()
      }
    case bool: BoolQueryBuilder =>
      bool.must().asScala.flatMap(findQueryTimes(fieldName)) ++
        bool.filter().asScala.flatMap(findQueryTimes(fieldName))
    case _ => Seq()
  }

  /**
    *
    * @param t the date formatted somehow
    * @return the date in milliseconds
    */
  def parseDate(t: FLTuple)
               (implicit now: LongSupplier): Seq[DateRange] = {
    val (d1, d2) = t

    dateRangeEvaluator(
      dateParser.parse(d2.apply, now),
      dateParser.parse(d1.apply, now)
    )
  }

  @tailrec
  def dateRangeEvaluator(d1: Long, d2: Long): Seq[DateRange] = {
    // Check whether the range is in a given parameter
    if (d2 - d1 < 0) {
      dateRangeEvaluator(d2, d1)
    } else {

      // Discard milliseconds in the range
      val dd1 = LocalDateTime.ofEpochSecond(d1 / 1000, 0, ZoneOffset.UTC)
      val dd2 = LocalDateTime.ofEpochSecond(d2 / 1000, 0, ZoneOffset.UTC)

      val drange = Duration.between(dd1, dd2)

      ChronoUnit.MONTHS.between(dd1, dd2)

      val range = (d2 - d1) - ((d2 - d1) % 1000)

      // If the range is an exact multiple of minutes, hours, days, weeks or
      // months, that is a FixedDateRange. But we only want the first match
      // the broadest. Because if it is one exact month then it is also 30 exact
      // days.

      val fixedTime: List[DateUnit] = fixrange.all
        .find { time: DateUnit =>
          time.len.between(dd1, dd2) > 0
        }
        .map { time: DateUnit =>
          DateUnitMultiple(
            time.len.between(dd1, dd2).toInt,
            time)
        }
        .toList

      val fromFixedPointInTime = fixpoint.all
        .filter { time: fixpoint.DateFixPoint => time.matches(dd1) }

      fixedTime ++ fromFixedPointInTime
    }
  }

  def now: LongSupplier = {
    val frozenTime = new Date().getTime

    () => frozenTime
  }

  case class FixedDateRange(multiple: Int, unit: DateUnit) extends DateRange

  // For Current month; last month; current week and so on
  case class FixStartDateRange(start: Date, fixpoint: DateFixPoint)
    extends DateRange


}
