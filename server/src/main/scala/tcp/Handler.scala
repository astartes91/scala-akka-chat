package tcp

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp._
import akka.util.ByteString
import db.models.{Provider, User}
import db.repository.UserRepository

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class Handler extends Actor with ActorLogging {

  override def receive: Receive = {

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

          sender() ! Write(ByteString("You successfully registered!"))
        }
      }

    case "close" =>
      log.debug("Closing connection")
      sender() ! Close
    case _: ConnectionClosed =>
      log.debug("Connection closed by server.")
      context stop self
    case PeerClosed => context stop self
  }
}
