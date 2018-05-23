package org.bibliarij.chat.tcp

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Kill}
import akka.io.Tcp._
import akka.util.ByteString
import org.bibliarij.chat.db.models.{Message, _}
import org.bibliarij.chat.db.repository._
import org.bibliarij.chat.{AuthorizationHandler, Constants}

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class TcpHandler extends Actor with ActorLogging {

  private var user: User = null

  override def receive: Receive = {

    case CommandFailed(w: Write) =>
      log.error("Failed to write request.")
      exit
    case Received(data) =>
      var response: String = data.decodeString("UTF-8")
      log.info(s"Received response: $response")

      if(response.startsWith(Constants.CRED_RESP)){

        handleAuthorization(response)
      } else if (response.startsWith(Constants.MSG)) {
        if (user != null){
          response = response.replace(Constants.MSG, "")
          val message: Message = Message(0, user.id, response, LocalDateTime.now())
          MessageRepository.addMessage(message)
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
    val credentialsStr: String = response.replace(Constants.CRED_RESP, "")
    val credentials: Array[String] = credentialsStr.split(" ")
    if (credentials.length == 2) {
      val login: String = credentials(0)
      val password: String = credentials(1)

      val result: (String, User) = AuthorizationHandler.handleAuthorization(login, password, Provider.TCP)
      val authorizationResult: String = result._1
      sender() ! Write(ByteString(authorizationResult))
      if(authorizationResult.startsWith(Constants.AUTH_SUCCESS)){
        this.user = result._2
        UserRepository.addAuthorizedUser(user.login, self, sender())
        sender() ! Write(ByteString(authorizationResult))

        val messages: Seq[org.bibliarij.chat.db.models.Message] = MessageRepository.getLast15Messages()
        messages.foreach(
          message => sender() ! Write(ByteString(s"${Constants.MSG}${MessageRepository.messageToString(message)}"))
        )
      } else if (authorizationResult.startsWith(Constants.AUTH_FAIL)) {
        sender() ! Write(ByteString(authorizationResult))
        exit
      }
    } else {
      sender() ! Write(ByteString("Something went wrong"))
      exit
    }
  }

  private def exit {
    if (user != null) {
      UserRepository.removeAuthorizedUser(this.user.login)
      this.user = null
    }
    sender() ! Close
    context stop self
    self ! Kill
  }
}
