package org.bibliarij.chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}

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

      // chat room many clients -> merge hub -> broadcasthub -> many clients
      val (chatSink, chatSource): (Sink[String, _], Source[String, _]) =
        MergeHub.source[String].toMat(BroadcastHub.sink[String])(Keep.both).run()

      //val broadcast: Broadcast[String] = Broadcast[String](2)

      /*val credentialsMessageFilter: Flow[String, String, NotUsed] = Flow[String].filter(_.startsWith("<CRED_RESP>"))
      val msgMessageFilter: Flow[String, String, NotUsed] = Flow[String].filter(_.startsWith("<MSG>"))

      val value: Sink[String, _] = BroadcastHub.sink[String](256)(Keep.right)
      Flow[Message].map({case TextMessage.Strict(text) =>       Future.successful(text)}).toMat(value)*/

      val webSocketService: Flow[Message, Message, _] =

        Flow[Message].mapAsync(1) {
          // transform websocket message to domain message (string)
          case TextMessage.Strict(text) =>       Future.successful(text)
        }
          //.via(broadcast)
          .via(Flow.fromSinkAndSource(chatSink, chatSource))
          .map[Message](string => TextMessage(string))

        /*Flow[Message].map {
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
      }*/
      handleWebSocketMessages(webSocketService)
    }
  }

  private def route: Route = rootRoute ~ getResRoute ~ wsRoute
}
