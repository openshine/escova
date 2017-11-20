package com.openshine.escova

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpRequest}
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.bytes.{BytesArray, BytesReference}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.{NamedXContentRegistry, XContentParser}
import org.elasticsearch.rest.RestRequest
import org.elasticsearch.rest.action.search.RestSearchAction
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.joda.time.Seconds

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object ElasticHelper {
  val searchModule = new SearchModule(Settings.EMPTY, false, java.util
    .Collections.emptyList())
  val registry = new NamedXContentRegistry(searchModule.getNamedXContents)
  val jsonHeaders: java.util.Map[String, java.util.List[String]] = Map(
    "Content-Type" -> List("application/json").asJava
  ).asJava

  def createRestRequest(request: HttpRequest): RestRequest = {
    val indices = Map("index" -> "i1,i2", "type" -> "typ1").asJava
    val url = request.uri.toRelative.toString()

    new RestRequest(
      registry,
      indices,
      url,
      jsonHeaders
    ) {
      override def method(): RestRequest.Method = request.method

      override def hasContent: Boolean = true

      override def content(): BytesReference = {
        requestToBytesArray(request.entity
          .asInstanceOf[HttpEntity.Strict])
      }

      override def uri(): String = url
    }
  }

  def requestToBytesArray(strict: HttpEntity.Strict): BytesReference = {
    val bb = strict.data.asByteBuffer
    val bytes = new Array[Byte](bb.remaining())
    bb.get(bytes, 0, bytes.length)

    new BytesArray(bytes)
  }

  def createSourceBuilder(request: HttpRequest): SearchSourceBuilder = {
    val restRequest = createRestRequest(request)
    val searchRequest = new SearchRequest()

    restRequest.withContentOrSourceParamParserOrNull(
      (parser: XContentParser) =>
        RestSearchAction.parseSearchRequest(
          searchRequest, restRequest, parser
        )
    )

    searchRequest.source()
  }

  implicit def requestMethodToES(i: HttpMethod): RestRequest.Method =
    RestRequest.Method.valueOf(i.value)
}
