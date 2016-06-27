package models

import scalikejdbc.TypeBinder

sealed trait Role

object Role {

  case object Administrator extends Role
  case object Master extends Role

  def valueOf(value: String): Role = value match {
    case "Administrator" => Administrator
    case "Master"    => Master
    case _ => null
  }

  implicit val typeBinder: TypeBinder[Role] = TypeBinder.string.map(valueOf)

}