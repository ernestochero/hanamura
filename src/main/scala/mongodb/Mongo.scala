package mongodb

import models.User
import org.bson.codecs.configuration.{ CodecProvider, CodecRegistry }
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import org.mongodb.scala.bson.codecs.{ DEFAULT_CODEC_REGISTRY, Macros }
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import zio.UIO
import scala.reflect.ClassTag
object Mongo {
  lazy val userCodecProvider: CodecProvider = Macros.createCodecProvider[User]()
  lazy val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(
      userCodecProvider
    ),
    DEFAULT_CODEC_REGISTRY
  )
  def mongoClient(uri: String): UIO[MongoClient] = UIO.succeed(MongoClient(uri))
  def database(dbname: String, mongoClient: MongoClient): UIO[MongoDatabase] =
    UIO.succeed((mongoClient.getDatabase(dbname).withCodecRegistry(codecRegistry)))
  def collection[T](db: MongoDatabase,
                    collectionName: String)(implicit c: ClassTag[T]): UIO[MongoCollection[T]] =
    UIO.succeed(db.getCollection[T](collectionName))

  def setupMongoConfiguration[T](uri: String, databaseName: String, collectionName: String)(
    implicit c: ClassTag[T]
  ): UIO[MongoCollection[T]] =
    for {
      mongoClient <- mongoClient(uri)
      database    <- database(databaseName, mongoClient)
      collection  <- collection[T](database, collectionName)
    } yield collection
}
