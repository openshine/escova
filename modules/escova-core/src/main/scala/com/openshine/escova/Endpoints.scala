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

  implicit def java(jValue: JValue): RestResponse = {
    new RestResponse {
      override def content(): BytesReference =
        new BytesArray(compact(render(jValue)))

      override def contentType(): String = "application/json"

      override def status(): RestStatus = RestStatus.OK
    }
  }
}
