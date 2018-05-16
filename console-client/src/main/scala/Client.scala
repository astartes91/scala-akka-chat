import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.io.StdIn

class Client(remoteAddress: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging {

  IO(Tcp)(actorSystem) ! Connect(remoteAddress)

  override def receive: Receive = {
    case CommandFailed(command: Tcp.Command) =>
      log.error(s"Failed to connect to ${remoteAddress.toString}")
      exit
    case Connected(remote, local) =>
      log.info(s"Successfully connected to $remote!")

      val connection: ActorRef = sender()
      connection ! Register(self)

      context become {
        case CommandFailed(w: Write) =>
          log.error("Failed to write request.")
          exit
        case Received(data) =>
          val response: String = data.decodeString("UTF-8")
          log.info(s"Received response: $response")

          if(response.startsWith("<CRED_REQ>")){
            println(response.replace("<CRED_REQ>", ""))

            val credentials: String = StdIn.readLine()
            connection ! Write(ByteString(s"<CRED_RESP>$credentials"))
          } else if(response.startsWith("<AUTH_SUCCESS>")) {
            println(response.replace("<AUTH_SUCCESS>", ""))
            val listener: ActorRef = context.actorOf(Props(classOf[ConsoleListener], connection, self))
            listener ! "listen"
          } else {
            println(response)
          }
        case "close" =>
          log.info("Exit, closing connection...")
          connection ! Close
          exit
        case _: ConnectionClosed =>
          log.info("Connection closed by server.")
          exit
      }
  }

  private def exit{
    context stop self
    self ! Kill
    actorSystem.terminate()
    System.exit(0)
  }
}

class ConsoleListener(connection: ActorRef, client: ActorRef) extends Actor {

  override def receive: Receive = {
    case "listen" =>

      var message: String = ""
      while (message != null && !message.equals("!q")) {
        println("Plz enter message or !q for quit")
        message = StdIn.readLine()
        if(message != null && !message.equals("!q")){
          connection ! Write(ByteString(s"<MSG>$message"))
        }
      }
      client ! "close"
      context stop self
      self ! Kill
  }
}
