package tcp

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp._
import akka.util.ByteString
import db.models.{Provider, User, UserAuthorization}
import db.repository.{UserAuthorizationRepository, UserRepository}

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class Handler extends Actor with ActorLogging {

  private var isAuthorized: Boolean = false

  override def receive: Receive = {

    case CommandFailed(w: Write) =>
      log.error("Failed to write request.")
    case Received(data) =>
      log.debug("Received response.")
      var response: String = data.decodeString("UTF-8")
      log.info(response)

      if(response.startsWith("<CRED_RESP>")){

        handleAuthorization(response)
      }

    case "close" =>
      log.debug("Closing connection")
      sender() ! Close
    case _: ConnectionClosed =>
      log.debug("Connection closed by server.")
      context stop self
    case PeerClosed => context stop self
  }

  private def handleAuthorization(response: String): Unit = {
    val credentialsStr: String = response.replace("<CRED_RESP>", "")
    val credentials: Array[String] = credentialsStr.split(" ")
    if (credentials.length == 2) {
      val login: String = credentials(0)
      val password: String = credentials(1)
      val userOpt: Option[User] = UserRepository.findByLogin(login)
      if (userOpt.nonEmpty) {
        val user: User = userOpt.get
        if (user.password.equals(password)) {
          isAuthorized = true
          UserAuthorizationRepository.insert(UserAuthorization(0, user.id, true, LocalDateTime.now()))
          sender() ! Write(ByteString("You successfully logged in!"))
        } else {
          UserAuthorizationRepository.insert(UserAuthorization(0, user.id, false, LocalDateTime.now()))
          sender() ! Write(ByteString("You are not authorized!"))
          sender() ! Close
        }
      } else {
        UserRepository.insert(
          User(0, Provider.TCP, None, login, password, true, LocalDateTime.now())
        )

        sender() ! Write(ByteString("<CRED_REQ>You successfully registered! Now you can log in."))
      }
    } else {
      sender() ! Write(ByteString("Something went wrong"))
    }
  }
}
