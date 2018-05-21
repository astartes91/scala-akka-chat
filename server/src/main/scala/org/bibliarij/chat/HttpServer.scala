package org.bibliarij.chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
//import akka.stream.scaladsl.FlowGraph.Implicits._
import org.bibliarij.chat.db.AuthorizationHandler
import org.bibliarij.chat.db.models.Provider

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
      redirect("web/index.html", StatusCodes.PermanentRedirect)
    }
  }

  private def getResRoute: Route =
    pathPrefix("web"){
      get {
        getFromResourceDirectory("web")
      }
    }

  private def wsRoute: Route = path("ws"){
    pathEndOrSingleSlash {
      val webSocketService: Flow[Message, Message, _] =
        /*Flow(Source.actorRef[org.bibliarij.chat.db.models.Message](bufferSize = 5, OverflowStrategy.fail)) {
          implicit builder =>
            chatSource =>

        }*/

        Flow[Message].map {
        case TextMessage.Strict(txt) =>
          if (txt.startsWith("<CRED_RESP>")){
            val credentialsStr: String = txt.replace("<CRED_RESP>", "")
            val credentials: Array[String] = credentialsStr.split(" ")
            if (credentials.length == 2) {
              val login: String = credentials(0)
              val password: String = credentials(1)
              val authorizationResult: String =
                AuthorizationHandler.handleAuthorization(login, password, Provider.WEB_SOCKET)
              TextMessage(authorizationResult)
            } else {
              TextMessage("Something went wrong")
            }
          } else {
            TextMessage(txt)
          }
        case _ => TextMessage("Message type unsupported")
      }
      handleWebSocketMessages(webSocketService)
    }
  }

  private def route: Route = rootRoute ~ getResRoute ~ wsRoute
}
