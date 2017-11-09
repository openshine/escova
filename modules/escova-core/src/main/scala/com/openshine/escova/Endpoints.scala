package com.openshine.escova

import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.rest.{RestResponse, RestStatus}
import org.elasticsearch.search.builder.SearchSourceBuilder

import scala.language.implicitConversions

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object Endpoints {
  import org.json4s._
  import JsonDSL.WithBigDecimal._
  import org.json4s.native.JsonMethods._

  def java(j: JValue): RestResponse = j

  object Searchv {
    def apply(searchSourceBuilder: SearchSourceBuilder): JValue = {
      Map(
        "cost" -> Parser.analyze(searchSourceBuilder).value
      )
    }
  }

  object DateParser {
    def apply(searchSourceBuilder: SearchSourceBuilder,
              fieldName: String): JValue = {
      val result = com.openshine.escova.DateParser.analyze(
        searchSourceBuilder, fieldName)

      Map[String, JValue](
        "_metadata" -> Map("fieldname" -> fieldName),
        "v1alpha/dates_range" -> result.map(_.toMap),
        "query_template" -> parse(searchSourceBuilder.toString(): String)
      )
    }
  }

  implicit def jValueToElasticSearchHandler(jValue: JValue): RestResponse = {
    new RestResponse {
      override def content(): BytesReference =
        new BytesArray(compact(render(jValue)))

      override def contentType(): String = "application/json"

      override def status(): RestStatus = RestStatus.OK
    }
  }
}
