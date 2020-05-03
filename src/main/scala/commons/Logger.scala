package commons
import org.log4s.getLogger
import org.log4s.Logger
object Logger {
  implicit final val logger: Logger = getLogger("Hanamura Logger")
}
