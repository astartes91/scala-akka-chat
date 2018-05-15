import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.io.StdIn

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
          log.debug("Received response.")
          val response: String = data.decodeString("UTF-8")

          if(response.startsWith("<CRED_REQ>")){
            println(response.replace("<CRED_REQ>", ""))

            val credentials: String = StdIn.readLine()
            connection ! Write(ByteString(s"<CRED_RESP>$credentials"))
          } else {
            println(response)
          }
          /*val message: String = StdIn.readLine()
          connection ! Write(ByteString(s"<MSG>$message"))*/
        case "close" =>
          log.debug("Closing connection")
          connection ! Close
        case _: ConnectionClosed =>
          log.debug("Connection closed by server.")
          context stop self
      }
  }
}
