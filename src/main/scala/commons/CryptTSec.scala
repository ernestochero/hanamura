package commons
import cats.effect.Sync
import tsec.cipher.symmetric._
import tsec.cipher.symmetric.jca.{ AES128CTR, SecretKey }
import tsec.common._
import cats.implicits._
import tsec.hashing.jca._
import java.util.Base64
case object Base64Error extends RuntimeException
class CryptTSec[F[_]](key: String)(implicit F: Sync[F]) {
  implicit val ctrStrategy: IvGen[F, AES128CTR] = AES128CTR.defaultIvStrategy[F]

  private val keyF: F[SecretKey[AES128CTR]] =
    base64(encodeBase64(key)).map(keyToSpec).flatMap(AES128CTR.buildKey[F])

  def keyToSpec(keyBytes: Array[Byte]): Array[Byte] =
    java.util.Arrays.copyOf(keyBytes.hash[SHA1], 16)

  def encodeBase64(s: String): String = Base64.getEncoder.encodeToString(s.utf8Bytes)

  def base64(s: String): F[Array[Byte]] =
    s.b64Bytes.liftTo[F](Base64Error)

  def encrypt(value: String): F[String] =
    for {
      key       <- keyF
      encrypted <- AES128CTR.genEncryptor[F].encrypt(PlainText(value.utf8Bytes), key)
    } yield (encrypted.content ++ encrypted.nonce).toB64String

  def decrypt(value: String): F[String] =
    for {
      key        <- keyF
      base64Byte <- base64(value)
      cypherText <- AES128CTR.ciphertextFromConcat(base64Byte).liftTo[F]
      decrypted  <- AES128CTR.genEncryptor[F].decrypt(cypherText, key)
    } yield decrypted.toUtf8String

}

object CryptTSec {
  def apply[F[_]: Sync](key: String): CryptTSec[F] = new CryptTSec[F](key)
}
