package tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

class TcpServer(address: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging {

  IO(Tcp)(actorSystem) ! Bind(self, address)

  override def receive: Receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Tcp server started at $localAddress")
      context.parent ! b
    case CommandFailed(_: Bind) =>
      log.error("Error!")
      context.stop(self)
      self ! Kill
      actorSystem.terminate()
      System.exit(0)
    case Connected(remoteAddress, localAddress) =>
      log.info(s"Tcp client from $remoteAddress connected!")
      val handler: ActorRef = context.actorOf(Props[Handler])
      val connection: ActorRef = sender()
      connection ! Register(handler)
      connection ! Write(
        ByteString(
          "<CRED_REQ>Hello! Please enter your credentials as login with password separated by space."
        )
      )
  }
}
