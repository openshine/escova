package com.openshine.escova

import java.security.{AccessController, PrivilegedAction}
import java.util

import com.openshine.escova.monads.ComplexityMeasureMonad
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.{NamedXContentRegistry, XContentParser}
import org.elasticsearch.rest.RestRequest
import org.elasticsearch.rest.action.search.RestSearchAction
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.aggregations.{AggregationBuilder,
  AggregatorFactories}
import org.elasticsearch.search.aggregations.bucket.histogram
.DateHistogramAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder

import scala.collection.JavaConverters._
import scala.language.higherKinds

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object Parser {
  private val fsum = monads.ComplexityMeasureMonad.liftA2(sum)
  private val fmul = monads.ComplexityMeasureMonad.liftA2(mul)

  def main(args: Array[String]): Unit = {
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
        "index" -> "index1,index2",
        "type" -> "type1"
      ).asJava,
      "/{index}/{type}/_search",
      headers.asJava
    ) {
      override def method() = RestRequest.Method.GET

      override def hasContent: Boolean = true

      override def content(): BytesReference = new BytesArray(
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
        """.stripMargin)

      override def uri(): String = "/index1,index2/type1/_search"
    }

    restRequest
      .withContentOrSourceParamParserOrNull(
        (parser: XContentParser) =>
          RestSearchAction.parseSearchRequest(
            searchRequest, restRequest, parser
          )
      )
    println(searchRequest)

    val complexity = analyze(searchRequest.source())


    println("Final complexity: ", complexity.value)

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
    val factoriesBuilder = AccessController.doPrivileged(
      new PrivilegedAction[AggregatorFactories.Builder] {
        override def run(): AggregatorFactories.Builder = {
          val f: java.lang.reflect.Field =
            classOf[AggregationBuilder].getDeclaredField("factoriesBuilder")

          f.setAccessible(true)
          // Because it seems we need a factory builder to build factories to
          // build
          // objects for some reason.

          f.get(ag).asInstanceOf[AggregatorFactories.Builder]
        }
      })

    factoriesBuilder.getAggregatorFactories.asScala
  }

  def analyze(agg: AggregationBuilder): ComplexityMeasure[Double] = {
    val currentAggLevel = analyzeAgg(agg)
    val nxL = getSubAggregations(agg).map(analyze)

    val nextLevel = nxL.fold(SimpleComplexityMeasure(1d))(fsum)

    println("All values are: ", nxL, " sum is", nextLevel)

    fmul(currentAggLevel, nextLevel)
  }

  def analyzeAgg(agg: AggregationBuilder): ComplexityMeasure[Double] = {
    println("Analyzing agg: ", agg.getClass)
    agg match {
      case agg: DateHistogramAggregationBuilder => analyzeDate(agg)
      case _ => SimpleComplexityMeasure(1d)
    }
  }

  def analyzeDate(agg: DateHistogramAggregationBuilder):
  ComplexityMeasure[Double] = {
    val complexityByRange: Map[Char, Double] = Map(
      's' -> 3600d,
      'm' -> 60d,
      'h' -> 1d,
      'd' -> 1 / 24,
      'M' -> 1 / 24 / 30,
      'q' -> 1 / 24 / 30 / 4,
      'y' -> 1 / 24 / 365
    )

    val expr = agg.toString

    SimpleComplexityMeasure(complexityByRange(expr.last))
  }

  @inline private def sum(a: Double, b: Double) = a + b

  @inline private def mul(a: Double, b: Double) = a * b
}

sealed trait ComplexityMeasure[A] {
  def value: A

  def map(f: A => A)(
    implicit a: Monad[ComplexityMeasure]
  ): ComplexityMeasure[A] = a.unit(f(value))

  def flatMap[B](f: A => ComplexityMeasure[B]):
  ComplexityMeasure[B] = f(value)

}

case class SimpleComplexityMeasure[A](_value: A) extends
  ComplexityMeasure[A] {

  override def value: A = _value
}


trait Monad[M[_]] {
  def unit[A](a: A): M[A]

  def bind[A, B](ma: M[A], bf: A => M[B]): M[B]
}

trait Applicative[F[_]] {
  def liftA2[A, B](f: (A, A) => B): (F[A], F[A]) => F[B]
}

package object monads {

  implicit object ComplexityMeasureMonad
    extends Monad[ComplexityMeasure] with Applicative[ComplexityMeasure] {
    def unit(a: Int) = SimpleComplexityMeasure(0)

    def bind[A, B](ma: ComplexityMeasure[A],
                   bf: (A) => ComplexityMeasure[B]):
    ComplexityMeasure[B] = ma.flatMap(bf)

    override def liftA2[A, B](f: (A, A) => B): (ComplexityMeasure[A],
      ComplexityMeasure[A]) => ComplexityMeasure[B] = {
      (a: ComplexityMeasure[A], b: ComplexityMeasure[A]) =>
        unit(f(a.value, b.value))
    }

    override def unit[A](a: A): ComplexityMeasure[A] = SimpleComplexityMeasure(
      a)
  }

}
