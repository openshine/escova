package com.openshine.escova

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.openshine.escova.endpoints.{ParseDate, Searchv}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.Try

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object EscovaHttpService extends App with ToStrict {
  implicit val system: ActorSystem = ActorSystem("escova")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val _searchvpath = (d: (Option[String], Option[String]) => Route) => {
    path(Segment / Segment / "_searchv")((c, i) => d(Some(c), Some(i))) ~
      path(Segment / "_searchv")(c => d(Some(c), None)) ~
      path("_searchv")(d(None, None))
  }

  val searchvpath = _searchvpath {
    (collection, index) =>
      context =>
        Source.single(context.request)
          .map { request =>
            import org.json4s.native.JsonMethods._
            val entity = Searchv(
              ElasticHelper.createSourceBuilder(request))

            HttpResponse(
              entity = HttpEntity(
                contentType = `application/json`,
                compact(render(entity))
              )
            )
          }
          .runWith(Sink.head)
          .flatMap(context.complete(_))
  }
  val proxy = {
    searchvpath ~
      path("_escova" / "parse_dates") {
        parameter("dateFieldName") { fieldName =>
          toStrictEntity(
            new FiniteDuration(5, TimeUnit.SECONDS)) {
            context =>
              Source.single(context.request)
                .map { request =>
                  import org.json4s.native.JsonMethods._

                  val entity = ParseDate(
                    ElasticHelper.createSourceBuilder(request),
                    fieldName)

                  HttpResponse(
                    entity = HttpEntity(
                      `application/json`,
                      compact(render(entity))))
                }
                .runWith(Sink.head)
                .flatMap(context.complete(_))
          }
        } ~ { context =>
          import org.json4s._
          import JsonDSL.WithBigDecimal._
          import org.json4s.native.JsonMethods._

          context.complete(
            HttpResponse(status = StatusCodes.BadRequest,
              entity = HttpEntity(
                compact(render(
                  Map[String, JValue](
                    "status" -> 400,
                    "error" -> "You must provide a dateFieldName parameter"
                  ))
                ))
            ))
        }
      }
  } ~ { context =>
    val request = context.request


    val flow = Http(system)
      .outgoingConnection(
        request.uri.authority.host.address(),
        8000)
    val handler = Source.single(context.request)
      .map(r => {
        r.withHeaders(headers.RawHeader("X-Proxy", "escova-uservice"))
        r.withUri(r.uri.toRelative)
      })
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))

    handler
  }

  val (interface, port) = {
    Try((args(0): String, args(1).toInt)).getOrElse {
      (
        Option(System.getenv("ESCOVA_BIND_ADDRESS"))
          .getOrElse("0.0.0.0"),
        Try(System.getenv("ESCOVA_BIND_PORT").toInt)
          .getOrElse(9000)
      )
    }
  }

  println(s"Bound at http://${interface}:${port}")

  val binding = Http(system).bindAndHandle(
    handler = proxy,
    interface = interface,
    port = port)

}
