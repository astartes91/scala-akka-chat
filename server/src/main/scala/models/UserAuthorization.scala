package models

import java.time.LocalDateTime

import models.Mapping.localDateTimeMapping
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

  override def * = (id, userId, isSuccess, creationDate) <>
    (UserAuthorization.tupled, UserAuthorization.unapply)
}
