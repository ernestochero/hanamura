package graphql
import models.User
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import zio.console.Console
import zio.{ IO, Queue, RIO, Ref, Task, UIO, ZIO }
import commons.Transformers._
import zio.stream.ZStream

import scala.concurrent.{ ExecutionContext, Future }
// pass ref of database for future implementation
class HanamuraService(userCollection: Ref[MongoCollection[User]],
                      subscribers: Ref[List[Queue[String]]]) {
  implicit val ec: ExecutionContext = ExecutionContext.global
  def sayHello: RIO[Console, String] =
    Future.successful("I'm Hanamura your backend service").toRIO
  def getUserFromDatabase: Task[List[User]] =
    userCollection.get.flatMap(c => {
      ZIO.fromFuture(
        implicit ec => c.find().toFuture().recoverWith { case e => Future.failed(e) }.map(_.toList)
      )
    })

  def getUserFromDatabase(id: String): RIO[Console, Option[User]] = {
    val _id    = new ObjectId(id)
    val filter = Document("_id" -> _id)
    userCollection.get.flatMap(
      _.find(filter).toFuture().recoverWith { case e => Future.failed(e) }.map(_.headOption).toRIO
    )
  }

  def addUser(name: String): RIO[Console, User] = {
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

  def userAddedEvent: ZStream[Any, Nothing, String] = ZStream.unwrap {
    for {
      queue <- Queue.unbounded[String]
      _     <- subscribers.update(queue :: _)
    } yield ZStream.fromQueue(queue)
  }

}

object HanamuraService {
  def make(userCollection: MongoCollection[User]): UIO[HanamuraService] =
    for {
      state       <- Ref.make(userCollection)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new HanamuraService(state, subscribers)
}
