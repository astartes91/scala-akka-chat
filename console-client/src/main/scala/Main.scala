import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}

object Main extends App {
  val system: ActorSystem = ActorSystem("Client")
  val server: ActorRef = system.actorOf(Props(classOf[Client], new InetSocketAddress("0.0.0.0", 40000), system))
}
