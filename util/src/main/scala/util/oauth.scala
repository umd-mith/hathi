package edu.umd.mith.util.oauth

import com.ning.http.client.FluentStringsMap
import com.ning.http.client.oauth.{
  ConsumerKey,
  OAuthSignatureCalculator,
  RequestToken
}
import com.ning.http.util.Base64
import dispatch.Req
import java.util.Random
import scala.collection.SortedMap

/** We call this "one-legged" OAuth, following
  * [[https://github.com/Mashape/mashape-oauth/blob/master/FLOWS.md#oauth-10a-one-legged
  * The OAuth Bible]] rather than the HathiTrust Data API documentation, which
  * calls it "two-legged".
  */
object OneLegged {
  def signed(key: String, secret: String)(req: Req)(params: (String, String)*) = {
    val signer = new OneLegged(key, secret)
    signer.sign(req, params.toMap)
  }
}

class OneLegged(key: String, secret: String)
  extends OAuthSignatureCalculator(
    new ConsumerKey(key, secret),
    new RequestToken("", "")
  )
  with NonceGenerator with ParamUtils {

  protected val rand = new Random(
    System.identityHashCode(this) + System.currentTimeMillis
  )

  private def oauthParams(timestamp: Long, nonce: String) = SortedMap(
    "oauth_consumer_key" -> key,
    "oauth_nonce" -> nonce,
    "oauth_signature_method" -> "HMAC-SHA1",
    "oauth_timestamp" -> timestamp.toString,
    "oauth_version" -> "1.0"
  )

  def sign(req: Req, queryParams: Map[String, String]) = {
    val nonce = generateNonce()
    val timestamp = System.currentTimeMillis() / 1000L

    val params = oauthParams(timestamp, nonce) ++ queryParams 

    val raw = rawBase("GET", req.url, sortAndConcat(params))
    val signature = Base64.encode(mac.digest(raw))

    req <<? params + ("oauth_signature" -> signature)
  }
}

trait NonceGenerator {
  private[this] val nonceBuffer = Array.fill[Byte](16)(0)

  protected def rand: Random

  def generateNonce() = nonceBuffer.synchronized {
    rand.nextBytes(nonceBuffer)
    Base64.encode(nonceBuffer)
  }
}

trait ParamUtils {
  import com.ning.http.util.UTF8Codec
  import com.ning.http.util.UTF8UrlEncoder.encode

  def sortAndConcat(params: SortedMap[String, String]) = params.mapValues(
    encode
  ).toSeq.map {
    case (k, v) => s"$k=$v"
  }.mkString("&")

  def rawBase(method: String, url: String, params: String) = UTF8Codec.toUTF8(
    s"$method&${ encode(url) }&${ encode(params)}"
  )
}
