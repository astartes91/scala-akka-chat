import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("Server")
  val tcpServer: ActorRef = system.actorOf(Props(classOf[TcpServer], new InetSocketAddress("0.0.0.0", 40000), system))

  implicit val flowMaterializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  new HttpServer("0.0.0.0", 8080, system, flowMaterializer, executionContext).start()
}
