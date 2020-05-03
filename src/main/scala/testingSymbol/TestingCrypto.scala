package testingSymbol
import zio.interop.catz._
import commons.CryptTSec
object TestingCrypto extends App {
  val privateKeyToEncrypt = "0186348C54DCFA1873218997F79BCBDC3D9684514CB1DD5E8103C45CE78F25D3"
  val passToEncrypt       = "P@ssword!"
  val cryptTsec           = CryptTSec[zio.Task](passToEncrypt)
  val process = for {
    encrypt <- cryptTsec.encrypt(privateKeyToEncrypt)
    decrypt <- cryptTsec.decrypt(encrypt)
  } yield (encrypt, decrypt)
  println(zio.Runtime.default.unsafeRunTask(process))
}
