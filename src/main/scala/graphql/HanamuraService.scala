package graphql
import models.User
import org.mongodb.scala.MongoCollection
import zio.{ Fiber, Queue, Ref, Task, UIO, ZIO }

import scala.concurrent.{ ExecutionContext, Future }
// pass ref of database for future implementation
class HanamuraService(userCollection: Ref[MongoCollection[User]],
                      subscribers: Ref[List[Queue[String]]]) {
  val ec: ExecutionContext = ExecutionContext.global
  def sayHello: Task[String] =
    ZIO.fromFuture(
      implicit ec => Future.failed(new Exception)
    )
  def getUserFromDatabase: Task[Seq[User]] =
    userCollection.get.flatMap(c => {
      ZIO.fromFuture(
        implicit ec => c.find().toFuture().recoverWith { case e => Future.failed(e) }.map(_.toList)
      )
    })

  def addUser(user: User): Task[User] =
    userCollection.get.flatMap(c => {
      ZIO.fromFuture(
        implicit ec =>
          c.insertOne(user).toFuture().recoverWith { case e => Future.failed(e) }.map(_ => user)
      )
    })
}

object HanamuraService {
  def make(userCollection: MongoCollection[User]): UIO[HanamuraService] =
    for {
      state       <- Ref.make(userCollection)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new HanamuraService(state, subscribers)
}
