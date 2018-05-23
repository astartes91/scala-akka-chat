package org.bibliarij.chat.websocket

import akka.actor._
import org.bibliarij.chat.db.models.Message

object ChatRoom {
  case object Join
  case class ChatMessage(message: String)
}

class ChatRoom extends Actor {
  import ChatRoom._
  var users: Set[ActorRef] = Set.empty

  def receive = {
    case Join =>
      users += sender()
      context.watch(sender())

    // we also would like to remove the user when its actor is stopped
    case Terminated(user) =>
      users -= user

    case msg: Message =>
      users.foreach(_ ! msg)
  }
}
