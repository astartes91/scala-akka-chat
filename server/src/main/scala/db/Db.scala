package db

import java.time.LocalDateTime

import db.models.Provider.Type
import db.models._
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Db {

  lazy implicit val providerMapping: JdbcType[Type] with BaseTypedType[Type] =
    MappedColumnType.base[Provider.Type, String](_.toString, Provider.withName)
  lazy implicit val localDateTimeMapping: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, String](_.toString, LocalDateTime.parse)

  lazy val users = TableQuery[UserTable]
  lazy val userAuthorizations = TableQuery[UserAuthorizationTable]

  lazy val db = Database.forConfig("databases.test-h2")
  Await.result(db.run((users.schema ++ userAuthorizations.schema).create), Duration.Inf)
}
