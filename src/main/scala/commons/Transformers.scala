package commons

import zio.{ RIO, Task, ZIO }

import scala.concurrent.Future
import io.reactivex.Observable
object Transformers {
  implicit final class FutureConverter[V](val self: Future[V]) {
    def toRIO: RIO[Any, V] =
      ZIO.fromFuture(implicit ec => self)
  }

  implicit final class ObservableJavaConverter[V](val self: Observable[V]) {
    def toTask: Task[V] = self.toFuture.toTask
  }

  implicit final class FutureJavaConverter[V](val self: java.util.concurrent.Future[V]) {
    def toTask: Task[V] =
      Task.effectSuspendTotal {
        if (self.isCancelled)
          Task.fail(new Exception("Is Cancelled"))
        else
          Task.succeed(self.get())
      }
  }

}
