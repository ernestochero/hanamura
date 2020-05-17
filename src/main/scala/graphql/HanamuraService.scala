package graphql

import commons.CryptTSec
import models.User
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import zio.{ Has, Queue, Ref, Task, UIO, ZIO, ZLayer }
import zio.interop.catz._
import commons.Transformers._
import io.nem.symbol.sdk.model.account.Address
import models.HanamuraManagementException.HanamuraAPIException
import scala.language.higherKinds
import scala.concurrent.{ ExecutionContext, Future }
object HanamuraService {
  type HanamuraServiceType = Has[Service]
  trait Service {
    def sayHello: UIO[String]
    def getUsersFromDatabase: Task[List[User]]
    def getUserFromDatabase(id: String): Task[Option[User]]
    def getPrivateKey(_id: ObjectId,
                      address: Address,
                      password: String): ZIO[HanamuraServiceType, Throwable, String]
  }
  def sayHello: ZIO[HanamuraServiceType, Nothing, String] =
    ZIO.accessM[HanamuraServiceType](_.get.sayHello)

  def getUsersFromDatabase: ZIO[HanamuraServiceType, Throwable, List[User]] =
    ZIO.accessM[HanamuraServiceType](_.get.getUsersFromDatabase)

  def getUserFromDatabase(id: String): ZIO[HanamuraServiceType, Throwable, Option[User]] =
    ZIO.accessM[HanamuraServiceType](_.get.getUserFromDatabase(id))

  def getPrivateKey(_id: ObjectId,
                    address: Address,
                    password: String): ZIO[HanamuraServiceType, Throwable, String] =
    ZIO.accessM[HanamuraServiceType](_.get.getPrivateKey(_id, address, password))

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
                .toTask
            )
          }
          /*          override def addUser(name: String): Task[User] = {
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
          }*/
          // implement method to getPrivateKey from DB
          override def getPrivateKey(
            _id: ObjectId,
            address: Address,
            password: String
          ): ZIO[HanamuraServiceType, Throwable, String] =
            for {
              uc <- userCollection.get
              filter = Document("_id" -> _id)
              users <- uc.find(filter).toFuture().toTask
              privateKey <- users.headOption.fold[Task[String]](
                ZIO.fail(HanamuraAPIException("user doesn't exist"))
              ) { user =>
                if (address.pretty() != user.address)
                  ZIO.fail(HanamuraAPIException("address doesn't match"))
                else ZIO.succeed(user.privateKeyEncrypted)
              }
              decrypt <- CryptTSec[Task](password).decrypt(privateKey)
            } yield decrypt
        }
    }
}
