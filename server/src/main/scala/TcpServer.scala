import java.net.InetSocketAddress
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import db.models.{Provider, User}
import db.repository.UserRepository

class TcpServer(address: InetSocketAddress, actorSystem: ActorSystem) extends Actor with ActorLogging {

  IO(Tcp)(actorSystem) ! Bind(self, address)

  override def receive: Receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Tcp server started at $localAddress")
      context.parent ! b
    case CommandFailed(_: Bind) =>
      log.error("Error!")
      context.stop(self)
    case Connected(remoteAddress, localAddress) =>
      log.info(s"Tcp client from $remoteAddress connected!")
      val connection: ActorRef = sender()
      connection ! Register(self)
      connection ! Write(
        ByteString(
          "<CRED_REQ>Hello! Please enter your credentials as login with password separated by space."
        )
      )

      context become {
        case CommandFailed(w: Write) =>
          log.error("Failed to write request.")
        case Received(data) =>
          log.debug("Received response.")
          var response: String = data.decodeString("UTF-8")
          log.info(response)

          if(response.startsWith("<CRED_RESP>")){
            response = response.replace("<CRED_RESP>", "")
            val credentials: Array[String] = response.split(" ")
            val login: String = credentials(0)
            val password: String = credentials(1)
            val user: Option[User] = UserRepository.findByLogin(login)
            if(user.nonEmpty){
              ???
            } else {
              UserRepository.insert(
                new User(0, Provider.TCP, None, login, password, true, LocalDateTime.now())
              )

              connection ! Write(ByteString("You successfully registered!"))
            }
          }

        case "close" =>
          log.debug("Closing connection")
          connection ! Close
        case _: ConnectionClosed =>
          log.debug("Connection closed by server.")
          context stop self
      }
  }
}
