package org.bibliarij.chat.websocket

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.bibliarij.chat.Loggable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn

class HttpServer(
                host: String,
                port: Int,
                implicit val system: ActorSystem,
                implicit val flowMaterializer: ActorMaterializer,
                implicit val executionContext: ExecutionContextExecutor
                ) extends Loggable {

  def start(){
    Await.result(Http().bindAndHandle(route, host, port), Duration.Inf)
    logger.info(s"Http server started at $host:$port, press enter to kill server")
    StdIn.readLine()
    system.terminate()
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

  /**
    * Main idea taken from https://markatta.com/codemonkey/blog/2016/04/18/chat-with-akka-http-websockets/
    * @return
    */
  private def wsRoute: Route = path("ws"){
    pathEndOrSingleSlash {

      val userActor: ActorRef = system.actorOf(Props(new UserActor()))

      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message].collect {
          // transform websocket message to domain message
          case TextMessage.Strict(text) => UserEvent.IncomingMessage(text)
        }.to(Sink.actorRef[UserEvent.IncomingMessage](userActor, PoisonPill))

      val outgoingMessages: Source[Message, NotUsed] =
        Source.actorRef[UserEvent.OutgoingMessage](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            // give the user actor a way to send messages out
            userActor ! UserEvent.Connected(outActor)
            NotUsed
          }.map(
          // transform domain message to web socket message
          (outMsg: UserEvent.OutgoingMessage) => TextMessage(outMsg.text))

      val webSocketService: Flow[Message, Message, _] = Flow.fromSinkAndSource(incomingMessages, outgoingMessages)

      handleWebSocketMessages(webSocketService)
    }
  }

  private def route: Route = rootRoute ~ getResRoute ~ wsRoute
}
