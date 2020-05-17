package models

import org.mongodb.scala.bson.ObjectId
case class User(_id: ObjectId = new ObjectId(),
                name: String,
                address: String,
                privateKeyEncrypted: String)
