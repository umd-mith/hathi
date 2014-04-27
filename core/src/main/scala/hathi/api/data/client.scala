package edu.umd.mith.hathi
package api
package data

import argonaut._, Argonaut._
import dispatch.url
import edu.umd.mith.hathi.mets.MetsFile
import scala.concurrent.Future
import scalaz.syntax.foldable._, scalaz.std.list._

/** The core client, responsible for making requests to the HathiTrust Data API.
  * Most of the hard work is done by [[edu.umd.mith.util.oauth.OneLegged]].
  */
trait DataClient extends BasicClient {
  def key: String
  def secret: String

  def base = "https://babel.hathitrust.org/cgi/htd"

  protected def signedReq(baseUrl: String, params: (String, String)*) =
    signOneLegged(key, secret)(url(baseUrl))((params :+ ("v" -> "2")): _*)

  protected def structureReq(id: Htid) =
    signedReq(s"$base/structure/$id", "format" -> "xml")

  def getStructureXml(id: Htid): Future[String] =
    stringWithRetries(backoffDelay, backoffMaxRetries)(structureReq(id))

  protected def aggregateReq(id: Htid) =
    signedReq(s"$base/aggregate/$id")

  def getAggregateBytes(id: Htid): Future[Array[Byte]] =
    bytesWithRetries(backoffDelay, backoffMaxRetries)(aggregateReq(id))

  protected def metadataReq(id: Htid) =
    signedReq(s"$base/volume/meta/$id", "format" -> "json")

  def getMetadataJson(id: Htid): Future[Json] =
    decodedJsonWithRetries[Json](backoffDelay, backoffMaxRetries)(metadataReq(id))

  protected def pageImageReq(id: Htid, seq: String) =
    signedReq(s"$base/volume/pageimage/$id/$seq", "format" -> "raw")

  def getPageImageBytes(id: Htid, seq: String): Future[Array[Byte]] =
    bytesWithRetries(backoffDelay, backoffMaxRetries)(pageImageReq(id, seq))

  protected def pageTextReq(id: Htid, seq: String) =
    signedReq(s"$base/volume/pageocr/$id/$seq")

  def getPageText(id: Htid, seq: String): Future[String] =
    stringWithRetries(backoffDelay, backoffMaxRetries)(pageTextReq(id, seq))

  protected def pageOcrReq(id: Htid, seq: String) =
    signedReq(s"$base/volume/pagecoordocr/$id/$seq")

  def getPageOcr(id: Htid, seq: String): Future[String] =
    stringWithRetries(backoffDelay, backoffMaxRetries)(pageOcrReq(id, seq))

  def getMetsFile(id: Htid): Future[MetsFile] =
    getStructureXml(id).flatMap {
      MetsFile.fromString(_).toEither match {
        case Right(metsFile) => Future.successful(metsFile)
        case Left(error) => Future.failed(error)
      }
    }

  def getPagesMetadata(id: Htid): Future[List[PageMetadata]] =
    getStructureXml(id).flatMap {
      MetsFile.fromString(_).map(_.pageMetadata).toEither match {
        case Right(pagesMetadata) => Future.successful(pagesMetadata)
        case Left(error) => Future.failed(error)
      }
    }
}
