package db.models

import java.time.LocalDateTime

import db.Db
import db.Db.localDateTimeMapping
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

case class Message(id: Int, userId: Int, text: String, createdAt: LocalDateTime)

class MessageTable(tag: Tag) extends Table[Message](tag, "messages") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def text = column[String]("text")
  def creationDate = column[LocalDateTime]("created_at")

  def userKey = foreignKey("MESSAGE_USER_FK", userId, Db.users)(_.id)

  override def * = (id, userId, text, creationDate) <> (Message.tupled, Message.unapply)
}
