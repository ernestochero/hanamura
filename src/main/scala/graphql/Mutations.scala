package graphql

import models.User
import zio.Task

case class nameArg(name: String)
case class Mutations(addUser: nameArg => Task[User])
