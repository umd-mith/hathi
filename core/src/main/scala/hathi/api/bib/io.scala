package edu.umd.mith.hathi.api.bib

import argonaut._, Argonaut._
import edu.umd.mith.hathi.Htid
import edu.umd.mith.util.PairtreeParser
import java.io.{ File, PrintWriter }
import scala.concurrent.Future

/** Additional client functionality for writing downloaded data to disk.
  */
trait BibClientIO { this: BibClient =>
  private[this] def initializeDir(outputDir: File): (File, File) = {
    if (!outputDir.exists) outputDir.mkdir()

    val resultsDir = new File(outputDir, "results")
    if (!resultsDir.exists) resultsDir.mkdir()

    (outputDir, resultsDir)
  }

  def download(htids: List[Htid], outputDir: File, checkIfDone: Boolean = true): Future[Unit] = {
    val (baseDir, resultsDir) = initializeDir(outputDir)

    val notDone = htids.filterNot {
      case htid @ Htid(_, _) =>
        val file = new File(resultsDir, s"${ htid.toFileName }.json")
        file.exists
    }

    getBibResults(notDone).map(saveResults(_, baseDir))
  }

  def saveResults(results: BibResultSet, outputDir: File): Unit = {
    val (baseDir, resultsDir) = initializeDir(outputDir)

    val missingWriter = new PrintWriter(new File(baseDir, "missing.txt"))
    results.missing.toList.sorted.foreach(htid => missingWriter.println(htid))
    missingWriter.close()

    val failedWriter = new PrintWriter(new File(baseDir, "failed.txt"))
    results.failed.foreach(_._1.foreach(htid => failedWriter.println(htid)))
    failedWriter.close()

    results.successful.foreach {
      case (htid @ Htid(_, _), result) =>
        val writer = new PrintWriter(
          new File(resultsDir, s"${ htid.toFileName }.json")
        )
        writer.println(result.asJson.spaces2)
        writer.close()
    }
  }
}
