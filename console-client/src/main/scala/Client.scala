import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill}
import akka.io.Tcp._
import akka.io.{IO, Tcp}

class Client(remoteAddress: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging {

  IO(Tcp)(actorSystem) ! Connect(remoteAddress)

  override def receive: Receive = {
    case CommandFailed(command: Tcp.Command) =>
      log.error(s"Failed to connect to ${remoteAddress.toString}")
      self ! Kill
      actorSystem.terminate()
    case Connected(remote, local) =>
      log.info(s"Successfully connected to $remote!")

      val connection: ActorRef = sender()
      connection ! Register(self)

      context become {
        case CommandFailed(w: Write) =>
          log.error("Failed to write request.")
        case Received(data) =>
          log.info("Received response.")
          println(data.decodeString("UTF-8"))
        case "close" =>
          log.info("Closing connection")
          connection ! Close
        case _: ConnectionClosed =>
          log.info("Connection closed by server.")
          context stop self
      }
  }
}
