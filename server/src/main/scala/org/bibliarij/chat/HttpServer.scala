package org.bibliarij.chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.ExecutionContextExecutor

class HttpServer(
                host: String,
                port: Int,
                implicit val system: ActorSystem,
                implicit val flowMaterializer: ActorMaterializer,
                implicit val executionContext: ExecutionContextExecutor
                ) extends Loggable {

  def start(){
    Http().bindAndHandle(route, host, port)
    logger.info(s"Http server started at $host:$port")
  }

  private def rootRoute: Route = pathSingleSlash {
    get {
      redirect("index.html", StatusCodes.PermanentRedirect)
    }
  }

  private def getResRoute: Route =
    pathPrefix(""){
      get {
        getFromResourceDirectory(".")
      }
    }

  private def wsRoute: Route = path("ws"){
    pathEndOrSingleSlash {
      val echoService: Flow[Message, Message, _] = Flow[Message].map {
        case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
        case _ => TextMessage("Message type unsupported")
      }
      handleWebSocketMessages(echoService)
    }
  }

  private def route: Route = rootRoute ~ getResRoute ~ wsRoute
}
