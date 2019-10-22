package graphql

import graphql.HanamuraServer.DatabaseConnection
import zio.{ Queue, Ref, UIO }
// pass ref of database for future implementation
class HanamuraService(databaseConnection: Ref[DatabaseConnection],
                      subscribers: Ref[List[Queue[String]]]) {
  def sayHello: UIO[String] = UIO.succeed("Hello I'm Hanamura [your backend service]")
}

object HanamuraService {
  def make(databaseConnection: DatabaseConnection): UIO[HanamuraService] =
    for {
      state       <- Ref.make(databaseConnection)
      subscribers <- Ref.make(List.empty[Queue[String]])
    } yield new HanamuraService(state, subscribers)
}
