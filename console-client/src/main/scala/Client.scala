import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorSystem, Kill}
import akka.io.Tcp.{CommandFailed, Connect, Connected}
import akka.io.{IO, Tcp}

class Client(remoteAddress: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging {

  IO(Tcp)(actorSystem) ! Connect(remoteAddress)

  override def receive: Receive = {
    case CommandFailed(command: Tcp.Command) =>
      log.error(s"Failed to connect to ${remoteAddress.toString}")
      self ! Kill
      actorSystem.terminate()
    case Connected(remote, local) => log.info(s"Successfully connected to $remote!")
  }
}
