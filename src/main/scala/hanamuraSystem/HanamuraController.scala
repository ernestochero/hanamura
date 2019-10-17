package hanamuraSystem

import akka.actor.ActorSystem
import scala.concurrent.Future

case class HanamuraController(system: ActorSystem) {
  def sayHello: Future[String] = Future.successful("Hello I'm Hanamura [your backend service]")
}
