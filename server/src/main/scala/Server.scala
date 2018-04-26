import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.io.Tcp._
import akka.io.{IO, Tcp}

class Server(address: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging{

  IO(Tcp)(actorSystem) ! Bind(self, address)

  override def receive: Receive = {
    case Bound(localAddress) => log.info(s"Server started at $localAddress")
    case CommandFailed(_: Bind) =>
      log.error("Error!")
      context.stop(self)
    case Connected(remoteAddress, localAddress) => log.info(s"Client from $remoteAddress connected!")
  }
}
