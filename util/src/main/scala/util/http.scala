package edu.umd.mith.util

import argonaut._, Argonaut._
import com.ning.http.client.Response
import dispatch._, retry.{ Backoff, Success }
import org.jboss.netty.util.Timer
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.Random

trait DispatchUtils {
  def http: Http
  /** Add a small random delay between actions.
    */
  def randomSleepFirst[A](maxMs: Int)(future: => Future[A])
    (implicit ec: ExecutionContext, timer: Timer): Future[A] =
      SleepFuture(Random.nextInt(maxMs).millisecond)(future).flatten

  /** We don't want to retry on 401 or 403 responses.
    */
  def authSuccess[A]: Success[Either[Throwable, A]] = new Success(
    {
      case Left(StatusCode(401)) => true
      case Left(StatusCode(403)) => true
      case Right(_) => true
      case _ => false
    }
  )

  def reqWithRetries(backoffDelay: Duration, backoffMaxRetries: Int)(req: Req)
    (implicit ec: ExecutionContext, timer: Timer): Future[Response] =
    Backoff(
      max = backoffMaxRetries,
      delay = backoffDelay
    )(() => http(req OK as.Response(identity)).either)(authSuccess, timer, ec).flatMap {
      case Left(error) => Future.failed(error)
      case Right(body) => Future.successful(body)
    }

  def stringWithRetries(backoffDelay: Duration, backoffMaxRetries: Int)(req: Req)
    (implicit ec: ExecutionContext, timer: Timer): Future[String] =
    reqWithRetries(backoffDelay, backoffMaxRetries)(req).map(as.String)

  def bytesWithRetries(backoffDelay: Duration, backoffMaxRetries: Int)(req: Req)
    (implicit ec: ExecutionContext, timer: Timer): Future[Array[Byte]] =
    reqWithRetries(backoffDelay, backoffMaxRetries)(req).map(as.Bytes)

  def decodedJsonWithRetries[A: DecodeJson]
    (backoffDelay: Duration, backoffMaxRetries: Int)(req: Req)
    (implicit ec: ExecutionContext, timer: Timer): Future[A] =
    reqWithRetries(backoffDelay, backoffMaxRetries)(req).flatMap { response =>
      Parse.decodeEither[A](as.String(response)).fold(
        msg => Future.failed(ArgonautError(msg)),
        Future.successful(_)
      )
    }

  def signOneLegged(key: String, secret: String)(req: Req)(params: (String, String)*) =
    oauth.OneLegged.signed(key, secret)(req)(params: _*)
}
