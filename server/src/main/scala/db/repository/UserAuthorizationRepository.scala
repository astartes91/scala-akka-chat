package db.repository

import db.Db
import db.models.UserAuthorization
import slick.jdbc.H2Profile.api._

object UserAuthorizationRepository {
  def insert(userAuthorization: UserAuthorization) {
    Db.db.run(Db.userAuthorizations += userAuthorization)
  }
}
