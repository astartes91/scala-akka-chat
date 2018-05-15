package tcp

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp._
import akka.util.ByteString
import db.models._
import db.repository.{MessageRepository, UserAuthorizationRepository, UserRepository}

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class Handler extends Actor with ActorLogging {

  private var user: User = null

  override def receive: Receive = {

    case CommandFailed(w: Write) =>
      log.error("Failed to write request.")
    case Received(data) =>
      var response: String = data.decodeString("UTF-8")
      log.debug(s"Received response: $response")

      if(response.startsWith("<CRED_RESP>")){

        handleAuthorization(response)
      } else if (response.startsWith("<MSG>")) {
        response = response.replace("<MSG>", "")
        MessageRepository.addMessage(Message(0, user.id, response, LocalDateTime.now()))
      }

    case "close" =>
      log.debug("Closing connection")
      user = null
      sender() ! Close
    case _: ConnectionClosed =>
      log.debug("Connection closed by server.")
      user = null
      context stop self
    case PeerClosed =>
      user = null
      context stop self
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
          this.user = user
          UserAuthorizationRepository.insert(UserAuthorization(0, user.id, true, LocalDateTime.now()))
          sender() ! Write(ByteString("You successfully logged in!"))

          val messages: Seq[db.models.Message] = MessageRepository.findAll()
          messages.foreach(
            message => {
              val loginOpt: Option[String] = UserRepository.findLoginById(message.userId)
              var login: String = ""
              if (loginOpt.nonEmpty){
                login = loginOpt.get
              } else {
                throw new Exception("Strange error")
              }
              sender() ! Write(ByteString(s"<MSG>time:${message.createdAt},author:$login,text:${message.text}"))
            }
          )
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
