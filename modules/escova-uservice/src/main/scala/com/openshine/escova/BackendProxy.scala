package com.openshine.escova

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes, headers}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Sink, Source}

import scala.util.Try

/**
  * @author Santiago Saavedra (ssaavedra@openshine.com)
  */
object BackendProxy {
  case class BackendHost(host: String, port: Int)

  val maybeHost: Either[String, BackendHost] = {
    val host = Option(System.getenv("ESCOVA_BACKEND_HOST"))
      .toRight("You must provide ESCOVA_BACKEND_HOST in the environment")
    val port = Try(System.getenv("ESCOVA_BACKEND_PORT").toInt)
      .toOption
      .toRight("You must provide a valid int as ESCOVA_BACKEND_PORT")

    host.right.flatMap(host =>
      port.right.map(port =>
        BackendHost(host, port))
    )
  }

  def route(implicit system: ActorSystem): Route = {
    maybeHost match {
      case Left(err) => context =>
        context.complete(HttpResponse(
        status = StatusCodes.BadGateway,
        entity = "Error: no backend available"
      ))
      case Right(backend) =>
        rightRoute(backend)
    }
  }

  def rightRoute(backend: BackendHost)
                (implicit system: ActorSystem): Route = { context =>
    val request = context.request

    val flow = Http(system)
      .outgoingConnection(
        backend.host,
        backend.port)

    Source.single(context.request)
      .map(r => {
        r.withHeaders(headers.RawHeader("X-Proxy", "escova-uservice"))
        r.withUri(r.uri.toRelative)
      })
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
  }

}
