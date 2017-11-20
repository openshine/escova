package com.openshine.escova

import com.openshine.escova.exceptions.NoSuchDateFieldException
import com.openshine.escova.fixpoint.FirstOfMonth
import com.openshine.escova.fixrange.{DateUnitMultiple, Month}
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder
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

    // println(dates)

    dates should equal(List(DateUnitMultiple(1, Month), FirstOfMonth))
  }


  "DateParser with an aggregate" should "add extended_bounds to the " +
    "aggregate" in {
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
    val agg: DateHistogramAggregationBuilder = n.source()
      .aggregations().getAggregatorFactories.get(0)
      .asInstanceOf[DateHistogramAggregationBuilder]

    agg.extendedBounds().toString should be("{{startTime}}--{{endTime}}")
  }

  "DateParser with a subaggregate" should
    "add extended_bounds to the aggregate" in {
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
        |    "1": {
        |      "terms": {
        |        "field": "x"
        |        },
        |      "aggs": {
        |        "2": {
        |          "date_histogram": {
        |            "field": "date",
        |            "interval": "month"
        |          }
        |        }
        |      }
        |    }
        |  }
        |}
      """.stripMargin, "index", "type")

    val dates = DateParser.analyze(n.source(), "date")

    val agg: DateHistogramAggregationBuilder = Parser.getSubAggregations(
      n.source().aggregations()
        .getAggregatorFactories.get(0)
    ).head.asInstanceOf[DateHistogramAggregationBuilder]

    agg.extendedBounds().toString should be("{{startTime}}--{{endTime}}")
  }

  "A query without any reference to a date field in the query" should "throw " +
    "an exception when trying to anaylze it" in {
    val n = Parser.parse(
      """
        |{
        |  "query": {
        |  },
        |  "aggs": {
        |    "1": {
        |      "terms": {
        |        "field": "x"
        |        },
        |      "aggs": {
        |        "2": {
        |          "date_histogram": {
        |            "field": "date",
        |            "interval": "month"
        |          }
        |        }
        |      }
        |    }
        |  }
        |}
      """.stripMargin, "index", "type")

    a[NoSuchDateFieldException] should be thrownBy
      DateParser.analyze(n.source(), "date")
  }

  "A query with a nested date field in the query" should "not throw " +
    "anything" in {
    val n = Parser.parse(
      """
        |{
        |  "aggs": {
        |    "byDay": {
        |      "aggs": {
        |        "crashes": {
        |          "filter": {
        |            "term": {
        |              "eventName.keyword": {
        |                "boost": 1,
        |                "value": "analytics:CrashLog"
        |              }
        |            }
        |          }
        |        }
        |      },
        |      "date_histogram": {
        |        "field": "@timestamp",
        |        "interval": "day",
        |        "keyed": false,
        |        "min_doc_count": 0,
        |        "offset": 0,
        |        "order": {
        |          "_key": "asc"
        |        }
        |      }
        |    },
        |    "max_crashes": {
        |      "max_bucket": {
        |        "buckets_path": [
        |          "byDay>crashes._count"
        |        ],
        |        "gap_policy": "skip"
        |      }
        |    }
        |  },
        |  "query": {
        |    "bool": {
        |      "adjust_pure_negative": true,
        |      "boost": 1,
        |      "disable_coord": false,
        |      "must": [
        |        {
        |          "term": {
        |            "eventName.keyword": {
        |              "boost": 1,
        |              "value": "Error"
        |            }
        |          }
        |        },
        |        {
        |          "range": {
        |            "@timestamp": {
        |              "lte": "2017-09-15T00:00:00.000Z",
        |              "gte": "2017-09-01T00:00:00.000Z"
        |            }
        |          }
        |        }
        |      ]
        |    }
        |  }
        |}
      """.stripMargin, "index"," type"
    )

    noException should be thrownBy DateParser.analyze(n.source(), "@timestamp")
  }

}
