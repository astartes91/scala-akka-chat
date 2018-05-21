package org.bibliarij.chat.db

import java.time.LocalDateTime

import org.bibliarij.chat.db.models.{Provider, User, UserAuthorization}
import org.bibliarij.chat.db.repository.{UserAuthorizationRepository, UserRepository}

object AuthorizationHandler {

  def handleAuthorization(login: String, password: String, provider: Provider.Type) = {
    val userOpt: Option[User] = UserRepository.findByLogin(login)
    if (userOpt.nonEmpty) {
      val user: User = userOpt.get
      if (user.password.equals(password)) {
        UserAuthorizationRepository.insert(UserAuthorization(0, user.id, true, LocalDateTime.now()))
        "<AUTH_SUCCESS>You successfully logged in!"
      } else {
        UserAuthorizationRepository.insert(UserAuthorization(0, user.id, false, LocalDateTime.now()))
        "<AUTH_FAILURE>You are not authorized!"
      }
    } else {
      UserRepository.insert(
        User(0, provider, None, login, password, true, LocalDateTime.now())
      )
      "<CRED_REQ>You successfully registered! Now you can log in. Plz enter your credentials"
    }
  }
}
