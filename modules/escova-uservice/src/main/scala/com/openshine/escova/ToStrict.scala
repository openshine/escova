package com.openshine.escova

import akka.http.scaladsl.model.HttpEntity.{ChunkStreamPart, Strict}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * https://gist.github.com/rklaehn/3aa3215046df2c0a7795
  */
trait ToStrict extends BasicDirectives with Directives {

  def makeStrict(timeout: FiniteDuration)(
      implicit fm: Materializer): Directive1[HttpRequest] = {
    extractStrict(timeout).flatMap { strictRequest =>
      mapRequest((r: HttpRequest) => strictRequest).tflatMap { u =>
        provide(strictRequest)
      }
    }
  }

  private def extractStrict(timeout: FiniteDuration)(
      implicit fm: Materializer): Directive1[HttpRequest] = {
    for {
      request <- extractRequest
      strictT <- onComplete(strictify(request, timeout))
    } yield {
      strictT match {
        case Success(strict) => request.copy(entity = strict)
        case Failure(scheisse) =>
          request
      }
    }
  }

  private def strictify(request: HttpRequest, duration: FiniteDuration)(
      implicit fm: Materializer): Future[Strict] = {
    request.entity match {
      case e @ HttpEntity.Strict(contentType: ContentType, data: ByteString) =>
        Future.successful(e)
      case e @ HttpEntity.Default(contentType: ContentType,
                                  contentLength: Long,
                                  data: Source[ByteString, Any]) =>
        e.toStrict(duration)
      case e @ HttpEntity.Chunked(contentType: ContentType,
                                  chunks: Source[ChunkStreamPart, Any]) =>
        e.toStrict(duration)
    }
  }
}
