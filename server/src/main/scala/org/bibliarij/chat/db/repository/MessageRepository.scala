package org.bibliarij.chat.db.repository

import java.util
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

import org.bibliarij.chat.db.Db
import org.bibliarij.chat.db.Db.localDateTimeMapping
import org.bibliarij.chat.db.models.Message
import slick.jdbc.H2Profile.api._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MessageRepository {

  private val messages: java.util.List[Message] = new CopyOnWriteArrayList[Message]

  def messageToString(message: Message) = {
    val login: String = UserRepository.findLoginById(message.userId).getOrElse(throw new Exception)
    s"Time:${message.createdAt} Author:$login Text:${message.text}"
  }

  def addMessage(message: Message): Unit = {
    messages.add(message)
    UserRepository.sendMessageToAllUsers(message)
  }

  def saveMessages() = {
    Await.result(Db.db.run(Db.messages ++= asScalaBuffer(messages)), Duration.Inf)
    messages.clear()
  }

  private def getLast15UnsavedMessages() = {
    val messages15: util.List[Message] =
      this.messages.stream()
        .sorted((m, m1) => m.createdAt.compareTo(m1.createdAt))
        .limit(15)
        .collect(Collectors.toList())
    val messageArray: Array[Message] = new Array[Message](messages15.size())
    messages15.toArray(messageArray)
  }

  private def findLast15SavedMessages(): Seq[Message] = {
    Await.result(Db.db.run(Db.messages.sortBy(m => m.creationDate).take(15).result), Duration.Inf)
  }

  def getLast15Messages(): Seq[Message] = {
    (findLast15SavedMessages() ++ getLast15UnsavedMessages())
      .sortWith((m, m1) => m.createdAt.isBefore(m1.createdAt))
      .take(15)
  }
}
