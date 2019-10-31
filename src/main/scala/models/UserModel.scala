package models

import org.mongodb.scala.bson.ObjectId

object UserModel {}
case class UserDomain(id: Option[String], name: String)
case class User(_id: ObjectId = new ObjectId(), name: String)
