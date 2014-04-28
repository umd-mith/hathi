package edu.umd.mith.util

import java.io.File
import scala.io.Source
import scalaz._, Scalaz._

object FileUtils {
  case class FileExistenceError(msg: String) extends Exception(msg)
}

trait FileUtils {
  def isDir(file: File): Throwable \/ File =
    if (file.exists && file.isDirectory) file.right else FileUtils.FileExistenceError(
      s"$file is not a directory."
    ).left

  def isFile(file: File): Throwable \/ File =
    if (file.exists && file.isFile) file.right else FileUtils.FileExistenceError(
      s"$file is not a file."
    ).left

  def contents(file: File): Throwable \/ String = \/.fromTryCatch(Source.fromFile(file).mkString)
}
