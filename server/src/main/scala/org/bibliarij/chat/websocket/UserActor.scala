package org.bibliarij.chat.websocket

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, Kill}
import org.bibliarij.chat.db.models.{Message, Provider, User}
import org.bibliarij.chat.db.repository.MessageRepository
import org.bibliarij.chat.websocket.UserEvent.{Connected, IncomingMessage, OutgoingMessage}
import org.bibliarij.chat.{AuthorizationHandler, Constants}

object UserEvent {
  case class Connected(outgoing: ActorRef)
  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}

class UserActor(chatRoom: ActorRef) extends Actor {

  private var user: User = null

  def receive = {
    case Connected(outgoing) => context.become(connected(outgoing))
  }

  def connected(outgoing: ActorRef): Receive = {
    case IncomingMessage(text) =>
      if (text.startsWith(Constants.CRED_RESP)){
        val credentialsStr: String = text.replace(Constants.CRED_RESP, "")
        val credentials: Array[String] = credentialsStr.split(" ")
        if (credentials.length == 2) {
          val login: String = credentials(0)
          val password: String = credentials(1)
          val authorizationResult: (String, User) =
            AuthorizationHandler.handleAuthorization(login, password, Provider.WEB_SOCKET)
          val authorizationResultMessage: String = authorizationResult._1
          outgoing ! OutgoingMessage(authorizationResultMessage)

          if (authorizationResultMessage.startsWith(Constants.AUTH_SUCCESS)){
            chatRoom ! ChatRoom.Join
            user = authorizationResult._2
            val messages: Seq[Message] = MessageRepository.getLast15Messages()
            messages.foreach(msg => outgoing ! OutgoingMessage(Constants.MSG + MessageRepository.messageToString(msg)))
          } else if (authorizationResultMessage.startsWith(Constants.AUTH_FAIL)){
            exit(outgoing)
          }
        } else {
          outgoing ! OutgoingMessage("Something went wrong")
          exit(outgoing)
        }
      } else if(text.startsWith(Constants.MSG)) {
        if (user == null){
          outgoing ! OutgoingMessage("You are not authorized!")
          exit(outgoing)
        }
        val messageText: String = text.replace(Constants.MSG, "")
        val message: Message = Message(0, user.id, messageText, LocalDateTime.now())
        chatRoom ! message
        MessageRepository.addMessage(message)
      }

    case msg: Message => outgoing ! OutgoingMessage(Constants.MSG + MessageRepository.messageToString(msg))
  }

  private def exit(outgoing: ActorRef): Unit = {
    sender() ! Kill
    self ! Kill
    outgoing ! Kill
  }
}
