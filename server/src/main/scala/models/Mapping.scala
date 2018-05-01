package models

import java.time.LocalDateTime

import models.Provider.Type
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

/**
  * @author Vladimir Nizamutdinov (astartes91@gmail.com)
  */
object Mapping {
  implicit val providerMapping: JdbcType[Type] with BaseTypedType[Type] =
    MappedColumnType.base[Provider.Type, String](_.toString, Provider.withName)
  implicit val localDateTimeMapping: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, String](_.toString, LocalDateTime.parse)
}
