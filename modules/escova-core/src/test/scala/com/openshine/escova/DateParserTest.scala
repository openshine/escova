package com.openshine.escova

import com.openshine.escova.fixpoint.FirstOfMonth
import com.openshine.escova.fixrange.{DateUnitMultiple, Month}
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
class DateParserTest extends FlatSpec with Matchers {

  "DateParser with an exact month" should "say it is either last month or " +
    "1-of-month-to-date" in {
    val n = Parser.parse(
      """
        |{
        |  "query": {
        |    "range": {
        |      "date": {
        |        "gte": "now-1M/M",
        |        "lte": "now/M"
        |      }
        |    }
        |  }
        |}
      """.stripMargin, "index", "type")

    val dates: Seq[DateRange] = DateParser.analyze(n.source(), "date")

    println(dates)

    dates should equal (List(DateUnitMultiple(1, Month), FirstOfMonth))
  }


  "DateParser with an aggregate" should "add extended_bounds to the aggregate" in {
    val n = Parser.parse(
      """
        |{
        |  "query": {
        |    "range": {
        |      "date": {
        |        "gte": "now-1M/M",
        |        "lte": "now/M"
        |      }
        |    }
        |  },
        |  "aggs": {
        |    "2": {
        |      "date_histogram": {
        |        "field": "date",
        |        "interval": "month"
        |      }
        |    }
        |  }
        |}
      """.stripMargin, "index", "type")

    val dates = DateParser.analyze(n.source(), "date")

    println(dates)

    val agg: DateHistogramAggregationBuilder = n.source()
      .aggregations().getAggregatorFactories.get(0)
      .asInstanceOf[DateHistogramAggregationBuilder]

    agg.extendedBounds().toString should be("{{startTime}}--{{endTime}}")
  }

}
