package db.repository

import db.Db
import db.models.User
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object UserRepository {

  def insert(user: User) {
    Db.db.run(Db.users += user)
  }

  def findByLogin(login: String): Option[User] = {
    Await.result(Db.db.run(Db.users.filter(_.login === login).result.headOption), Duration.Inf)
  }

  def findLoginById(id: Int): Option[String] = {
    Await.result(Db.db.run(Db.users.filter(_.id === id).map(_.login).result.headOption), Duration.Inf)
  }
}
