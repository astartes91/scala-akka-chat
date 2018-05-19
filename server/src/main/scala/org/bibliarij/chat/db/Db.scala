package org.bibliarij.chat.db

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.bibliarij.chat.Init.system.dispatcher
import org.bibliarij.chat.db.models.Provider.Type
import org.bibliarij.chat.db.models._
import org.bibliarij.chat.db.repository.MessageRepository
import org.bibliarij.chat.{Init, Loggable}
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

object Db extends Loggable {

  lazy implicit val providerMapping: JdbcType[Type] with BaseTypedType[Type] =
    MappedColumnType.base[Provider.Type, String](_.toString, Provider.withName)
  lazy implicit val localDateTimeMapping: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, String](_.toString, LocalDateTime.parse)

  lazy val users = TableQuery[UserTable]
  lazy val userAuthorizations = TableQuery[UserAuthorizationTable]
  lazy val messages = TableQuery[MessageTable]

  lazy val db = Database.forConfig("databases.test-h2")
  Await.result(db.run((users.schema ++ userAuthorizations.schema ++ messages.schema).create), Duration.Inf)

  startScheduler()

  def startScheduler(): Unit = {
    logger.info("Starting scheduler...")

    val actor: ActorRef = Init.system.actorOf(Props(classOf[MessagesSavingActor]))
    Init.system.scheduler.schedule(0 milliseconds, 1 seconds, actor, "SAVE")
  }
}

class MessagesSavingActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case "SAVE" =>
      log.info("Saving messages...")
      MessageRepository.saveMessages()
  }
}
