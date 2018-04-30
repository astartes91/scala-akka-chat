package models

import java.time.LocalDateTime

import slick.jdbc.H2Profile.api._
import slick.lifted.{ProvenShape, Tag}

object Provider extends Enumeration {

  type Type = Value

  val TCP = Value
}

case class User(
                 id: Int,
                 provider: Provider.Type,
                 externalUserId: Option[String],
                 login: String,
                 password: String,
                 isOnline: Boolean,
                 registrationDate: LocalDateTime
               )

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
class UserTable(tag: Tag)
  extends Table[User](tag, "users") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def provider = column[Provider.Type]("provider")
  def externalUserId = column[Option[String]]("user_id")
  def login = column[String]("login")
  def password = column[String]("password")
  def isOnline = column[Boolean]("is_online")
  def registrationDate = column[LocalDateTime]("registered_at")

  override def * : ProvenShape[(Int, Provider.Type, Option[String], String, String, Boolean, LocalDateTime)] =
    (id, provider, externalUserId, login, password, isOnline, registrationDate) <> (User.tupled, User.unapply)
}
