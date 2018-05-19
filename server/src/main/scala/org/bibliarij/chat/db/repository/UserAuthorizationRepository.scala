package org.bibliarij.chat.db.repository

import org.bibliarij.chat.db.Db
import org.bibliarij.chat.db.models.UserAuthorization
import slick.jdbc.H2Profile.api._

object UserAuthorizationRepository {
  def insert(userAuthorization: UserAuthorization) {
    Db.db.run(Db.userAuthorizations += userAuthorization)
  }
}
