package graphql

import models.User
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import zio.{ Has, Queue, Ref, Task, UIO, ZIO, ZLayer }
import commons.Transformers._
import io.nem.symbol.sdk.model.account.Address
import zio.stream.ZStream

import scala.language.higherKinds
import scala.concurrent.{ ExecutionContext, Future }
object HanamuraService {
  type HanamuraServiceType = Has[Service]
  trait Service {
    def sayHello: UIO[String]
    def getUsersFromDatabase: Task[List[User]]
    def getUserFromDatabase(id: String): Task[Option[User]]
    def addUser(name: String): Task[User]
    def userAddedEvent: ZStream[Any, Nothing, String]
    def getPrivateKey(address: Address): ZIO[HanamuraServiceType, Nothing, String]
  }
  def sayHello: ZIO[HanamuraServiceType, Nothing, String] =
    ZIO.accessM[HanamuraServiceType](_.get.sayHello)

  def getUsersFromDatabase: ZIO[HanamuraServiceType, Throwable, List[User]] =
    ZIO.accessM[HanamuraServiceType](_.get.getUsersFromDatabase)

  def getUserFromDatabase(id: String): ZIO[HanamuraServiceType, Throwable, Option[User]] =
    ZIO.accessM[HanamuraServiceType](_.get.getUserFromDatabase(id))

  def addUser(name: String): ZIO[HanamuraServiceType, Throwable, User] =
    ZIO.accessM[HanamuraServiceType](_.get.addUser(name))

  def userAddedEvent =
    ZStream.accessStream[HanamuraServiceType](_.get.userAddedEvent)

  def getPrivateKey(address: Address): ZIO[HanamuraServiceType, Nothing, String] =
    ZIO.accessM[HanamuraServiceType](_.get.getPrivateKey(address))

  def make(userCollection: MongoCollection[User]): ZLayer[Any, Nothing, Has[Service]] =
    ZLayer.fromEffect {
      for {
        userCollection <- Ref.make(userCollection)
        subscribers    <- Ref.make(List.empty[Queue[String]])
      } yield
        new Service {
          implicit val ec: ExecutionContext  = ExecutionContext.global
          override def sayHello: UIO[String] = ZIO.succeed("I'm Hanamura your backend service")
          override def getUsersFromDatabase: Task[List[User]] =
            userCollection.get.flatMap(c => {
              ZIO.fromFuture(
                implicit ec =>
                  c.find().toFuture().recoverWith { case e => Future.failed(e) }.map(_.toList)
              )
            })
          override def getUserFromDatabase(id: String): Task[Option[User]] = {
            val _id    = new ObjectId(id)
            val filter = Document("_id" -> _id)
            userCollection.get.flatMap(
              _.find(filter)
                .toFuture()
                .recoverWith { case e => Future.failed(e) }
                .map(_.headOption)
                .toRIO
            )
          }
          override def addUser(name: String): Task[User] = {
            val user = User(name = name)
            val res = userCollection.get.flatMap(
              _.insertOne(user)
                .toFuture()
                .recoverWith { case e => Future.failed(e) }
                .map(_ => user)
                .toRIO
            )
            for {
              user <- res
              _ <- subscribers.get.flatMap(
                // add item to all subscribers
                UIO.foreach(_)(
                  queue =>
                    queue
                      .offer(user._id.toHexString)
                      .onInterrupt(subscribers.update(_.filterNot(_ == queue))) // if queue was shutdown, remove from subscribers
                )
              )
            } yield user
          }
          override def userAddedEvent: ZStream[Any, Nothing, String] = ZStream.unwrap {
            for {
              queue <- Queue.unbounded[String]
              _     <- subscribers.update(queue :: _)
            } yield ZStream.fromQueue(queue)
          }

          // implement method to getPrivateKey from DB
          override def getPrivateKey(address: Address): ZIO[HanamuraServiceType, Nothing, String] =
            ZIO.succeed("291D8F1111DE464C1DACF5CDFA722C104F458C7055D1119078018565EE76626A")
        }
    }
}
