package tcp

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Kill}
import akka.io.Tcp._
import akka.util.ByteString
import db.models._
import db.repository._

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class Handler extends Actor with ActorLogging {

  private var user: User = null

  override def receive: Receive = {

    case CommandFailed(w: Write) =>
      log.error("Failed to write request.")
      exit
    case Received(data) =>
      var response: String = data.decodeString("UTF-8")
      log.info(s"Received response: $response")

      if(response.startsWith("<CRED_RESP>")){

        handleAuthorization(response)
      } else if (response.startsWith("<MSG>")) {
        if (user != null){
          response = response.replace("<MSG>", "")
          MessageRepository.addMessage(Message(0, user.id, response, LocalDateTime.now()))
        } else {
          sender() ! Write(ByteString("You are not authorized!"))
          exit
        }
      }

    case "close" =>
      log.info("Closing connection")
      exit
    case _: ConnectionClosed =>
      log.info("Connection closed.")
      exit
    case PeerClosed =>
      log.info("Peer closed.")
      exit
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
          sender() ! Write(ByteString("<AUTH_SUCCESS>You successfully logged in!"))

          val messages: Seq[db.models.Message] = MessageRepository.getLast15Messages()
          messages.foreach(
            message => {
              val loginOpt: Option[String] = UserRepository.findLoginById(message.userId)
              var login: String = ""
              if (loginOpt.nonEmpty){
                login = loginOpt.get
              } else {
                throw new Exception("Strange error")
                exit
              }
              sender() ! Write(ByteString(s"<MSG>time:${message.createdAt},author:$login,text:${message.text}"))
            }
          )
        } else {
          UserAuthorizationRepository.insert(UserAuthorization(0, user.id, false, LocalDateTime.now()))
          sender() ! Write(ByteString("You are not authorized!"))
          exit
        }
      } else {
        UserRepository.insert(
          User(0, Provider.TCP, None, login, password, true, LocalDateTime.now())
        )

        sender() ! Write(ByteString(
          "<CRED_REQ>You successfully registered! Now you can log in. Plz enter your credentials"
        ))
      }
    } else {
      sender() ! Write(ByteString("Something went wrong"))
      exit
    }
  }

  private def exit {
    this.user = null
    sender() ! Close
    context stop self
    self ! Kill
  }
}
