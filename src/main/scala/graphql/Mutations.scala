package graphql

import models.User
import zio.Task

case class Mutations(addUser: User => Task[User])
