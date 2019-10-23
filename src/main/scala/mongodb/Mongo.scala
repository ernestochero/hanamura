package mongodb

import com.typesafe.config.{ Config, ConfigFactory }
import models.User
import org.bson.codecs.configuration.{ CodecProvider, CodecRegistry }
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import org.mongodb.scala.bson.codecs.{ DEFAULT_CODEC_REGISTRY, Macros }
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
object Mongo {
  lazy val userCodecProvider: CodecProvider = Macros.createCodecProvider[User]()
  lazy val config: Config                   = ConfigFactory.load()
  lazy val mongoClient: MongoClient         = MongoClient(config.getString("mongo.uri"))
  lazy val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(
      userCodecProvider
    ),
    DEFAULT_CODEC_REGISTRY
  )
  lazy val database: MongoDatabase =
    mongoClient.getDatabase(config.getString("mongo.database")).withCodecRegistry(codecRegistry)
  lazy val usersCollection: MongoCollection[User] =
    database.getCollection[User]("users-hanamura")
}
