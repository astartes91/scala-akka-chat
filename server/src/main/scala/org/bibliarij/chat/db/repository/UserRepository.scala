package org.bibliarij.chat.db.repository

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString
import org.bibliarij.chat.db.Db
import org.bibliarij.chat.db.models.{Message, User}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object UserRepository {

  private val authorizedUsers: java.util.Map[String, (ActorRef, ActorRef)] =
    new ConcurrentHashMap[String, (ActorRef, ActorRef)]()

  def addAuthorizedUser(login: String, handler: ActorRef, sender: ActorRef): Unit ={
    if (authorizedUsers.containsKey(login)){
      val prevUserActor: ActorRef = authorizedUsers.get(login)._1
      prevUserActor ! "close"
      removeAuthorizedUser(login)
    }
    authorizedUsers.put(login, (handler, sender))
  }

  def removeAuthorizedUser(login: String): Unit ={
    authorizedUsers.remove(login)
  }

  def sendMessageToAllUsers(message: Message): Unit = {
    authorizedUsers.values().forEach(_._2 !  Write(ByteString(s"<MSG>${MessageRepository.messageToString(message)}")))
  }

  def insert(user: User) {
    Await.result(Db.db.run(Db.users += user), Duration.Inf)
  }

  def findByLogin(login: String): Option[User] = {
    Await.result(Db.db.run(Db.users.filter(_.login === login).result.headOption), Duration.Inf)
  }

  def findLoginById(id: Int): Option[String] = {
    Await.result(Db.db.run(Db.users.filter(_.id === id).map(_.login).result.headOption), Duration.Inf)
  }
}
