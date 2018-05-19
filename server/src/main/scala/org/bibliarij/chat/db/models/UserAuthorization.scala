package org.bibliarij.chat.db.models

import java.time.LocalDateTime

import org.bibliarij.chat.db.Db
import org.bibliarij.chat.db.Db.localDateTimeMapping
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
case class UserAuthorization(id: Int, userId: Int, isSuccess: Boolean, createdAt: LocalDateTime)

class UserAuthorizationTable(tag: Tag) extends Table[UserAuthorization](tag, "users_auths") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def isSuccess = column[Boolean]("is_success")
  def creationDate = column[LocalDateTime]("created_at")

  def userKey = foreignKey("AUTHORIZATION_USER_FK", userId, Db.users)(_.id)

  override def * = (id, userId, isSuccess, creationDate) <>
    (UserAuthorization.tupled, UserAuthorization.unapply)
}
