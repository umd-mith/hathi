package edu.umd.mith.hathi.api
package bib
package cli

import dispatch.{ Defaults, Http }
import edu.umd.mith.hathi.Htid
import java.io.File
import scala.io.Source
import scala.concurrent.duration.Duration
import scala.concurrent.Await

/** A simple but useful example of how to use the Bib API client. The first
  * command-line argument should be a file containing a list of HathiTrust
  * volume identifiers, and the second should be the desired output directory.
  * If the output directory exists, a "results" subdirectory will be checked for
  * existing JSON files, which will not be downloaded again.
  */
object BibDownloader extends App
  with BasicClient with BibClient with BibClientIO {
  implicit val timer = Defaults.timer
  implicit val executor = Defaults.executor

  val http = Http
  val volumeListFile = new File(args(0))
  val outputDir = new File(args(1))

  val volumeListSource = Source.fromFile(volumeListFile)
  val volumeList = volumeListSource.getLines.map(Htid.parse).toList
  volumeListSource.close()

  val result = download(volumeList, outputDir)

  result.onFailure {
    case throwable: Throwable => println(throwable)
  }

  Await.ready(result, Duration.Inf)
  shutdown()
}
