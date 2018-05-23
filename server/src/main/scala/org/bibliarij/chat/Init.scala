package org.bibliarij.chat

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import org.bibliarij.chat.tcp.TcpServer
import org.bibliarij.chat.websocket.HttpServer

import scala.concurrent.ExecutionContextExecutor

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
object Init {

  implicit val system: ActorSystem = ActorSystem("Server")

  def init(): Unit = {

    val tcpServer: ActorRef = system.actorOf(Props(classOf[TcpServer], new InetSocketAddress("0.0.0.0", 40000), system))

    implicit val flowMaterializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    new HttpServer("0.0.0.0", 8080, system, flowMaterializer, executionContext).start()
  }
}
