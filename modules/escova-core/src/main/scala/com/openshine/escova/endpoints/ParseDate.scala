package com.openshine.escova.endpoints

import org.elasticsearch.search.builder.SearchSourceBuilder
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods.parse

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object ParseDate {
  def apply(searchSourceBuilder: SearchSourceBuilder,
            fieldName: String): JValue = {
    val result =
      com.openshine.escova.DateParser.analyze(searchSourceBuilder, fieldName)

    Map[String, JValue](
      "_metadata" -> Map("fieldname" -> fieldName),
      "v1alpha/dates_range" -> result.map(_.toMap),
      "query_template" -> parse(searchSourceBuilder.toString(): String)
    )
  }
}
