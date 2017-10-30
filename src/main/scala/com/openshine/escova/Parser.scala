package com.openshine.escova

import java.util

import com.openshine.escova.functional.{ComplexityMeasure,
  SimpleComplexityMeasure, implicits}
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.common.joda.DateMathParser
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.{NamedXContentRegistry, XContentParser}
import org.elasticsearch.rest.RestRequest
import org.elasticsearch.rest.action.search.RestSearchAction
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.aggregations.bucket.histogram
.DateHistogramAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.terms
.{TermsAggregationBuilder, TermsAggregator}
import org.elasticsearch.search.aggregations.{AggregationBuilder,
  AggregatorFactories}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.joda.time.DateTimeZone

import scala.collection.JavaConverters._

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object Parser {
  private val fsum = functional.monads.ComplexityMeasureMonad.liftA2(sum)
  private val fmul = functional.monads.ComplexityMeasureMonad.liftA2(mul)

  def main(args: Array[String]): Unit = {
    val searchRequest = parse(
      """
        |{
        |  "aggs": {
        |    "titles": {
        |      "terms": {
        |        "field": "title"
        |      },
        |      "meta": {
        |        "color": "blue"
        |      }
        |    },
        |    "my_unbiased_sample": {
        |      "diversified_sampler": {
        |        "shard_size": 200,
        |        "field": "author"
        |      },
        |      "aggs": {
        |        "keywords": {
        |          "significant_terms": {
        |            "field": "tags",
        |            "exclude": [
        |              "elasticsearch"
        |            ]
        |          }
        |        }
        |      }
        |    }
        |  }
        |}
      """.stripMargin, "index1,index2", "type1")

    val complexity = analyze(searchRequest.source())

    println("Final complexity: ", complexity.value)


  }

  def parse(search_string: String,
            indices: String,
            types: String
           ): SearchRequest = {

    val searchModule = new SearchModule(Settings.EMPTY, false,
      util.Collections.emptyList())
    val searchRequest = new SearchRequest()

    val xContentRegistry = new NamedXContentRegistry(
      searchModule.getNamedXContents
    )
    val headers: Map[String, util.List[String]] = Map(
      "Content-Type" -> List("application/json").asJava
    )

    val restRequest = new RestRequest(
      xContentRegistry,
      Map(
        "index" -> indices,
        "type" -> types
      ).asJava,
      "/{index}/{type}/_search",
      headers.asJava
    ) {
      override def method() = RestRequest.Method.GET

      override def hasContent: Boolean = true

      override def content(): BytesReference = new BytesArray(search_string)

      override def uri(): String = s"/$indices/$types/_search"
    }

    restRequest
      .withContentOrSourceParamParserOrNull(
        (parser: XContentParser) =>
          RestSearchAction.parseSearchRequest(
            searchRequest, restRequest, parser
          )
      )

    searchRequest
  }

  def analyze(n: SearchSourceBuilder): ComplexityMeasure[Double] = {
    val aggs = Option(n).flatMap { n => Option(n.aggregations()) }

    aggs.map(_.getAggregatorFactories.asScala.map(analyze))
      .map(_.fold(SimpleComplexityMeasure(0d))(fsum))
      .getOrElse(SimpleComplexityMeasure(-1d))
  }

  /**
    * Hackity hack. For some reason, the Elastic team has not provided the
    * only useful method in all the AST: recursion.
    *
    * @param ag the aggregation builder you want to descend to
    * @return the aggregation builders beneath `ag`
    */
  def getSubAggregations(ag: AggregationBuilder): Seq[AggregationBuilder] = {
    import implicits._

    val factoriesBuilder =
      ag.getPrivateFieldValue[AggregatorFactories.Builder]("factoriesBuilder")

    factoriesBuilder.getAggregatorFactories.asScala
  }

  def analyze(agg: AggregationBuilder): ComplexityMeasure[Double] = {
    val currentAggLevel = analyzeAgg(agg)
    val nxL = getSubAggregations(agg).map(analyze)

    val nextLevel = nxL.fold(SimpleComplexityMeasure(1d))(fsum)
    fmul(currentAggLevel, nextLevel)
  }

  def termsComplexityFactor(t: TermsAggregationBuilder)
  : ComplexityMeasure[Double] = {
    import implicits._
    val requiredSize = t.getPrivateFieldValue[TermsAggregator
    .BucketCountThresholds]("bucketCountThresholds")
      .getRequiredSize

    SimpleComplexityMeasure(math.pow(10, requiredSize / 10))
  }

  def analyzeAgg(agg: AggregationBuilder): ComplexityMeasure[Double] = {
    println("Analyzing agg: ", agg.getClass)
    agg match {
      case agg: DateHistogramAggregationBuilder => analyzeDate(agg)
      case agg: TermsAggregationBuilder => termsComplexityFactor(agg)
      case _ => SimpleComplexityMeasure(1d)
    }
  }

  def dateMathExpressionToSeconds(dateExpr: String): Long = {
    val equivalences = Map(
      "SECOND" -> "1s", "MINUTE" -> "1m", "HOUR" -> "1h", "DAY" -> "1d",
      "WEEK" -> "1w", "MONTH" -> "1M", "QUARTER" -> "1q", "YEAR" -> "1y",
    )

    val expr = equivalences.getOrElse(dateExpr.toUpperCase, dateExpr)

    val dateParser = new DateMathParser(DateParser.DEFAULT_DATE_TIME_FORMATTER)

    dateParser.parse(s"now+$expr", () => 0L, false, DateTimeZone.UTC)
  }

  def analyzeDate(agg: DateHistogramAggregationBuilder)
  : ComplexityMeasure[Double] = {
    val dateExpr = agg.dateHistogramInterval.toString
    SimpleComplexityMeasure(1000 / dateMathExpressionToSeconds(dateExpr))
  }

  @inline private def sum(a: Double, b: Double) = a + b

  @inline private def mul(a: Double, b: Double) = a * b
}
