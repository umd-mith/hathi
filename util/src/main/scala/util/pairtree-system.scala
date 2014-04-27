package edu.umd.mith.util

import java.io.File
import scalaz._, Scalaz._
import scalaz.concurrent._

/** Provides access to a Pairtree file system.
  */
trait PairtreeSystem {
  def parser: PairtreeParser
  def base: File

  def directory(id: String): Throwable \/ File = {
    val dir = parser.toPathParts(id).foldLeft(base) {
      case (acc, next) => new File(acc, next)
    }

    if (dir.exists && dir.isDirectory) dir.right else
      PairtreeParser.InvalidIdError(s"$id does not have a directory.").left
  }
}

class DefaultPairtreeSystem(val base: File) extends PairtreeSystem {
  val parser = PairtreeParser.Default
}
