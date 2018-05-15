package db.repository

import java.util.concurrent.CopyOnWriteArrayList

import db.Db
import db.models.Message
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MessageRepository {

  private val messages: CopyOnWriteArrayList[Message] = new CopyOnWriteArrayList[Message]

  def addMessage(message: Message): Unit = {
    messages.add(message)
  }

  def findAll(): Seq[Message] = {
    Await.result(Db.db.run(Db.messages.result), Duration.Inf)
  }
}
