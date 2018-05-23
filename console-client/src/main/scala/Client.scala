import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.io.StdIn

object Constants {
  val CRED_REQ: String = "<CRED_REQ>"
  val MSG: String = "<MSG>"
  val CRED_RESP: String = "<CRED_RESP>"
  val AUTH_SUCCESS: String = "<AUTH_SUCCESS>"
}

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

          if(response.startsWith(Constants.CRED_REQ)){
            println(response.replace(Constants.CRED_REQ, ""))

            val credentials: String = StdIn.readLine()
            connection ! Write(ByteString(s"${Constants.CRED_RESP}$credentials"))
          } else if(response.startsWith(Constants.AUTH_SUCCESS)) {
            println(response.replace(Constants.AUTH_SUCCESS, ""))
            val listener: ActorRef = context.actorOf(Props(classOf[ConsoleListener], connection, self))
            listener ! "listen"
          } else if(response.startsWith(Constants.MSG)) {
            println(response.replace(Constants.MSG, ""))
          } else {
            println(response)
          }
        case "close" =>
          log.info("Exit, closing connection...")
          exit
        case _: ConnectionClosed =>
          log.info("Connection closed by server.")
          exit
      }
  }

  private def exit{
    sender() ! Close
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
      val quitValue: String = "!q"
      while (message != null && !message.equals(quitValue)) {
        println("Plz enter message or " + quitValue + " for quit")
        message = StdIn.readLine()
        if(message != null && !message.equals(quitValue)){
          connection ! Write(ByteString(s"${Constants.MSG}$message"))
        }
      }
      client ! "close"
      context stop self
      self ! Kill
  }
}
