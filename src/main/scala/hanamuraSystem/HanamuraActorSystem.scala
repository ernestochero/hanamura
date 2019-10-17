package hanamuraSystem

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object HanamuraActorSystem {
  implicit val system       = ActorSystem("sangria-hanamura-server")
  implicit val materializer = ActorMaterializer()

  // here put your actor  ... cheers !
}
