package edu.umd.mith.hathi
package api
package data
package cli

import dispatch.{ Defaults, Http }
import edu.umd.mith.hathi.Htid
import java.io.File
import scala.io.Source
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

/** A simple example of how to use the Data API client. The first command-line
  * argument should be a HathiTrust volume identifier, and the second should be
  * and output directory. The application will attempt to download the metadata
  * and aggregate ZIP file for the volume.
  *
  * Note that this expects you to have a ".hathitrustrc" file with your consumer
  * key and consumer secret in your home directory; change the configuration
  * mixed in for other options.
  *
  * Note also that this will not work on Google-digitized volumes if you do not
  * have specific authorization to access these volumes.
  */
object DataDownloader extends App
  with BasicClient with DataClient with DataClientIO with DataClientConfig {
  implicit val timer = Defaults.timer
  implicit val executor = Defaults.executor

  val http = Http
  val htid = Htid.parse(args(0))

  val outputDir = new File(args(1))
  if (!outputDir.exists) outputDir.mkdir()

  val result = for {
    metsFile <- getMetsFile(htid)
    _ <- Future(metsFile.save(new File(outputDir, s"${ htid.toFileName }.mets.xml")))
    _ <- saveAggregate(htid)(new File(outputDir, s"${ htid.toFileName }.zip"))
  } yield ()

  result.onFailure {
    case throwable: Throwable => println(throwable)
  }

  Await.ready(result, Duration.Inf)
  shutdown()
}
