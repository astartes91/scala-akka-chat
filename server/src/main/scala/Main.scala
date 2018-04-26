import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}

object Main extends App {
  val system = ActorSystem("Server")
  val server = system.actorOf(Props(classOf[Server], new InetSocketAddress("0.0.0.0", 40000), system))
}
