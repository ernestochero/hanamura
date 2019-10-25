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
      implicit ec => Future.successful("I'm Hanamura your backend service")
    )
  def getUserFromDatabase: Task[List[User]] =
    userCollection.get.flatMap(c => {
      ZIO.fromFuture(
        implicit ec => c.find().toFuture().recoverWith { case e => Future.failed(e) }.map(_.toList)
      )
    })

  def addUser(name: String): Task[User] = {
    val user = User(name = name)
    userCollection.get.flatMap(c => {
      ZIO.fromFuture(
        implicit ec =>
          c.insertOne(user)
            .toFuture()
            .recoverWith { case e => Future.failed(e) }
            .map(_ => user)
      )
    })
  }

}

object HanamuraService {
  def make(userCollection: MongoCollection[User]): UIO[HanamuraService] =
    for {
      state       <- Ref.make(userCollection)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new HanamuraService(state, subscribers)
}
