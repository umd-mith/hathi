package edu.umd.mith.hathi.api
package bib

import argonaut._, Argonaut._
import dispatch.{ UriEncode, url }
import edu.umd.mith.hathi.Htid
import scala.concurrent.Future
import scalaz.syntax.foldable._, scalaz.std.list._

/** The core client, responsible for making requests to the HathiTrust Bib API.
  */
trait BibClient extends BasicClient {
  private[this] lazy val HtidPattern = "^htid:(.+)$".r

  def base = "http://catalog.hathitrust.org/api/volumes/full/json/"

  protected def batchReq(batch: List[Htid]) = url(
    base + batch.map(htid => s"htid:$htid").mkString(UriEncode.path("|"))
  )

  def getBibResults(htids: List[Htid]): Future[BibResultSet] = {
    val batches = htids.grouped(20).toList
    
    Future.traverse(batches) { batch =>
      val req = batchReq(batch)

      randomSleepFirst(1000)(
        decodedJsonWithRetries[Map[String, BibResult]](
          backoffDelay,
          backoffMaxRetries
        )(req).map { htidMap =>
          val successful = htidMap.flatMap {
            // If the item list is empty, that means the volume is missing.
            case (_, BibResult(_, Nil)) => None
            case (HtidPattern(id), res) => Some(Htid.parse(id) -> res)
          }
          val missing = batch.toSet -- successful.keySet

          BibResultSet.success(successful, missing)
        }.recover {
          case error: Throwable => BibResultSet.failure(batch, error)
        }
      )
    }.map(_.suml)
  }
}
