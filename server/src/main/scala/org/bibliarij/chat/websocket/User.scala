package chat

import akka.actor.{Actor, ActorRef, Kill}
import org.bibliarij.chat.db.models.Provider
import org.bibliarij.chat.{AuthorizationHandler, Constants}

object User {
  case class Connected(outgoing: ActorRef)
  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}

class User(chatRoom: ActorRef) extends Actor {
  import User._

  def receive = {
    case Connected(outgoing) =>
      context.become(connected(outgoing))
  }

  def connected(outgoing: ActorRef): Receive = {
    case IncomingMessage(text) =>
      if (text.startsWith(Constants.CRED_RESP)){
        val credentialsStr: String = text.replace(Constants.CRED_RESP, "")
        val credentials: Array[String] = credentialsStr.split(" ")
        if (credentials.length == 2) {
          val login: String = credentials(0)
          val password: String = credentials(1)
          val authorizationResult: String =
            AuthorizationHandler.handleAuthorization(login, password, Provider.WEB_SOCKET)
          outgoing ! OutgoingMessage(authorizationResult)

          if (authorizationResult.startsWith(Constants.AUTH_SUCCESS)){
            chatRoom ! ChatRoom.Join
          } else if (authorizationResult.startsWith(Constants.AUTH_FAIL)){
            exit(outgoing)
          }
        } else {
          outgoing ! OutgoingMessage("Something went wrong")
          exit(outgoing)
        }
      } else if(text.startsWith(Constants.MSG)) {
        val message: String = text.replace(Constants.MSG, "")
        chatRoom ! ChatRoom.ChatMessage(message)
      }

    case ChatRoom.ChatMessage(text) => outgoing ! OutgoingMessage(Constants.MSG + text)
  }

  private def exit(outgoing: ActorRef): Unit = {
    sender() ! Kill
    self ! Kill
    outgoing ! Kill
  }
}
