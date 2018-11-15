package com.openshine.escova

import java.util

import com.openshine.escova.functional.{CostMeasure, SimpleCostMeasure, implicits}
import com.openshine.escova.nodes._
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.common.joda.DateMathParser
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.{NamedXContentRegistry, XContentParser}
import org.elasticsearch.rest.RestRequest
import org.elasticsearch.rest.action.search.RestSearchAction
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.terms.{TermsAggregationBuilder, TermsAggregator}
import org.elasticsearch.search.aggregations.{AggregationBuilder, AggregatorFactories}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.joda.time.DateTimeZone

import scala.collection.JavaConverters._

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object Parser {

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

    implicit val defaultConfig = CostConfig(default = NodeCostConfig(
      whitelistChildren = List("date_histogram", "terms"),
      maxTreeHeight = 3,
    ),
      custom = Map(
        "terms" -> NodeCostConfig(
          customNodeConfig = Map(
            "bucketCountThresholds.requiredSize.op" -> "pow"
          ),
          whitelistChildren = List("date_histogram")
        ),
        "date_histogram" -> NodeCostConfig(
          whitelistChildren = List("terms")
        )
      )
    )

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

  def analyze(n: SearchSourceBuilder)(implicit costConfig: CostConfig): CostMeasure[Double] = {
    val aggs = Option(n).flatMap(n => Option(n.aggregations()))

    val rootAgg = aggs.map(_
      .getAggregatorFactories
      .asScala
      .map(generateTree)
      .foldLeft(RootAggregation())(_ :+ _))
      .map(computeTreeCost)

    rootAgg.getOrElse(SimpleCostMeasure(0d))
  }

  def generateTree(agg: AggregationBuilder): TreeNode = {
    val children = getSubAggregations(agg).map(generateTree)
    children.foldLeft[TreeNode](
      LeafAggregation(agg, Double.NaN)
    )(_ :+ _)
  }

  def computeInnerTreeCost(tree: TreeNode)(implicit config: CostConfig): TreeNode = {
    tree match {
      case SubAggregation(node, cost, children) =>
        val computedChildren = children.map(computeInnerTreeCost)
        val withChildren = SubAggregation(node, cost, computedChildren)
        SubAggregation(node, getNodeCost(config, withChildren), computedChildren)

      case tree @ LeafAggregation(node, cost) =>
        LeafAggregation(node, getNodeCost(config, tree))
    }
  }

  def computeTreeCost(tree: AnalysisTree)(implicit config: CostConfig): AnalysisTree = {
    tree match {
      case RootAggregation(children, cost) =>
        val computedChildren = children.map(computeInnerTreeCost)
        getRootCost(config, computedChildren)

      case tree: TreeNode =>
        computeInnerTreeCost(tree)
    }
  }

  def getRootCost(config: CostConfig, children: Seq[TreeNode]): RootAggregation = {
    val configForNode = config.custom.getOrElse("root", config.default)

    if (children.exists(t => !configForNode.isChildAllowed(t))) {
      // Fail fast on blacklisted children
      return RootAggregation(children.toVector, Double.MaxValue)
    }

    val childrenCost = children
      .map(_.cost)
      .foldLeft(1d)(
        binaryOp(configForNode.nodeCostOpAggChild)
      )
    val totalNodeCost = binaryOp(
      configForNode.childrenCostOp
    )(configForNode.defaultNodeCost, childrenCost)

    RootAggregation(children.toVector, totalNodeCost)
  }

  def getNodeCost(config: CostConfig, tree: AnalysisTree with TreeNode): Double = {
    val configForNode = config.custom.getOrElse(tree.node.getName, config.default)

    if (tree.children.exists(t => !configForNode.isChildAllowed(t))) {
      // Fail fast on blacklisted children
      return Double.MaxValue
    }

    val nodeCost = tree.node match {
      case node: TermsAggregationBuilder =>
        import implicits._
        val requiredSize = node
          .getPrivateFieldValue[TermsAggregator.BucketCountThresholds]("bucketCountThresholds")
          .getRequiredSize

        val op = configForNode.customNodeConfig.getOrElse("bucketCountThresolds.requiredSize.op", "pow")

        if(op == "pow") {
          math.pow(10, requiredSize / 10)
        } else {
          throw new RuntimeException("TermsAggregationBuilder not correctly configured: missing" +
            "bucketCountThresolds.requiredSize.op property.")
        }

      case agg: DateHistogramAggregationBuilder =>
        val dateExpr = agg.dateHistogramInterval.toString
        1000 / dateMathExpressionToSeconds(dateExpr)

      case agg: DateRangeAggregationBuilder =>
        import scala.collection.JavaConverters._
        agg
          .ranges()
          .asScala
          .map(range =>
            analyzeRangeDates("to", range) -
              analyzeRangeDates("from", range))
          .sum

      case _ => configForNode.defaultNodeCost
    }

    val childrenCost = tree.children.map(_.cost).foldLeft(1d)(binaryOp(configForNode.nodeCostOpAggChild))
    val totalNodeCost = binaryOp(configForNode.childrenCostOp)(nodeCost, childrenCost)

    totalNodeCost
  }

  def binaryOp[T](op: String)(x: T, y: T)(implicit num: Fractional[T])
  : T = op match {
    case "sum" => num.plus(x, y)
    case "minus" => num.minus(x, y)
    case "mul" => num.times(x, y)
    case "avg" =>
      import num.mkNumericOps
      (x + y) / num.fromInt(2)
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

  def analyzeRangeDates(field: String, range: RangeAggregator.Range): Double = {
    import implicits._
    field match {
      case "to" =>
        var res = range.getPrivateFieldValue[Double](field)
        if (res.isInfinity) res = analyzeRangeDates("toAsStr", range)
        res
      case "from" =>
        var res = range.getPrivateFieldValue[Double](field)
        if (res.isInfinity) res = analyzeRangeDates("fromAsStr", range)
        res
      case _ =>
        val dateParser = new DateMathParser(DateParser.DEFAULT_DATE_TIME_FORMATTER)

        val millis_in_a_day = 1000 * 3600 * 24 * 7
        val dateExpr = range.getPrivateFieldValue[String](field)
        dateParser.parse(dateExpr, () => 0L, false, DateTimeZone.UTC) / millis_in_a_day
    }
  }

  def dateRangeAggComplexityFactor(t: DateRangeAggregationBuilder)
  : CostMeasure[Double] = {
    import scala.collection.JavaConverters._
    var cost = 0d
    for (range <- t.ranges().asScala) cost += analyzeRangeDates("to", range) - analyzeRangeDates("from", range)
    SimpleCostMeasure(cost)
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
  : CostMeasure[Double] = {
    val dateExpr = agg.dateHistogramInterval.toString
    SimpleCostMeasure(1000 / dateMathExpressionToSeconds(dateExpr))
  }

  @inline private def sum(a: Double, b: Double) = a + b

  @inline private def mul(a: Double, b: Double) = a * b
}
