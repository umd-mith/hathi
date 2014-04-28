package edu.umd.mith.util

import argonaut._, Argonaut._
import java.net.URL
import org.apache.commons.lang.StringEscapeUtils.unescapeJavaScript
import scala.concurrent.duration.Duration
import scalaz.\/
import scalaz.concurrent.Task

object ArgonautUtils {
  case class ArgonautError(msg: String) extends Exception(msg)
}

trait ArgonautUtils {

  def disjunctionToResult[A](h: CursorHistory)(t: Throwable \/ A): DecodeResult[A] =
    t.fold(
      e => DecodeResult.fail(e.getMessage, h),
      DecodeResult.ok
    )

  def tryResult[A](h: CursorHistory)(a: => A): DecodeResult[A] =
    disjunctionToResult(h)(\/.fromTryCatch(a))

  def taskResult[A](h: CursorHistory)(t: Task[A]): DecodeResult[A] =
    disjunctionToResult(h)(t.get.run)

  implicit val URLCodecJson: CodecJson[URL] = CodecJson(
    (a: URL) => Json.jString(a.toString),
    (c: HCursor) => c.as[String].map(unescapeJavaScript).flatMap(s =>
      tryResult(c.history)(new URL(s))
    )
  )
}

