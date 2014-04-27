package edu.umd.mith.util

import java.io.File
import java.util.zip.ZipFile
import scala.collection.JavaConverters._
import scala.io.Source
import scalaz.concurrent.Task

trait ZipFileUtils {
  def zipFileContents(file: File): Task[List[(String, String)]] = Task {
    val zipFile = new ZipFile(file)

    val entries = zipFile.entries.asScala.map { entry =>
      val source = Source.fromInputStream(zipFile.getInputStream(entry))
      val contents = source.getLines.mkString("\n")
      source.close()

      entry.getName -> contents
    }.toList.sortBy(_._1).tail

    zipFile.close()

    entries
  }
}

