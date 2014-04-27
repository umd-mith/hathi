package edu.umd.mith.util

import scalaz.\/, scalaz.syntax.id._, scalaz.syntax.std.boolean._

/** Provides a Pairtree parser with a default configuration (and other utility
  * methods.
  */
object PairtreeParser {
  val HexIndicator = "^"
  val Default = PairtreeParser(java.io.File.separatorChar, 2)
  val LibraryAndId = "([^\\.]+)\\.(.*)".r

  case class InvalidIdError(msg: String) extends Exception(msg)
  case class InvalidPathError(msg: String) extends Exception(msg)

  /** A simple helper method to split a library prefix off of an identifier.
    */
  def toLibraryAndId(idWithLibrary: String): Throwable \/ (String, String) =
    idWithLibrary match {
      case LibraryAndId(library, id) => (library, id).right
      case _ => InvalidIdError(
        s""""$idWithLibrary" does not have a library component."""
      ).left
    }
}

/** Provides methods for working with Pairtree structures given a
  * configuration.
  */
case class PairtreeParser(separator: Char, shortyLength: Int) {
  import PairtreeParser._

  private[this] val WithoutFinalSeparator = f"(|.*[^$separator%c])$separator%c?".r

  private[this] def concat(paths: List[String]): String = paths.map {
    case WithoutFinalSeparator(path) => path
  }.mkString(separator.toString)

  def pathToCleanId(path: String): String = path.replace(separator.toString, "")

  def toPathParts(id: String): List[String] = cleanId(id).grouped(shortyLength).toList

  def toPath(id: String): String = concat(toPathParts(id))
  
  def toPathWithBase(basePath: String, id: String, encapsulatingDir: String): String =
    concat(List(basePath, toPath(id), encapsulatingDir))

  def toId(path: String): Throwable \/ String = path match {
    case WithoutFinalSeparator(id) => encapsulatingDir(path).flatMap(dir =>
      uncleanId(id.dropRight(dir.fold(0)(_.length)).replace(separator.toString, ""))
    )
  }

  def toIdWithBase(basePath: String)(path: String): Throwable \/ String =
    toId(removeBasepath(basePath, path))

  def encapsulatingDirWithBase(basePath: String)(path: String): Throwable \/ Option[String] =
    encapsulatingDir(removeBasepath(basePath, path))
  
  def encapsulatingDir(path: String): Throwable \/ Option[String] =
    path.split(separator).reverse.toList match {
      case part :: Nil if part.length <= shortyLength => None.right
      case last :: nextToLast :: rest =>      
        /** All parts up to next-to-last and last should have shorty length.
          * The next-to-last part should have shorty length or less.
          */
        val wrongLengthsInRest = rest.filterNot(_.size == shortyLength)
        val wrongLengths = if (nextToLast.length > shortyLength) {
          nextToLast :: wrongLengthsInRest
        } else wrongLengthsInRest

        wrongLengthsInRest.nonEmpty.either(
          InvalidPathError(
            s""""$path" has parts of incorrect length: """ +
            s"${wrongLengths.reverse.map("\"" + _ + "\"").mkString(", ")}."
          )
        ).or {
          val nextToLastIsShorter = nextToLast.length < shortyLength
          val lastIsLong = last.length > shortyLength

          (nextToLastIsShorter || lastIsLong).option(last)
        }
      case _ => 
        InvalidPathError(s""""$path" contains no shorties.""").left
    }

  def removeBasepath(basePath: String, path: String): String =
    basePath match {
      case WithoutFinalSeparator(base) if path.startsWith(base + separator) =>
        path.substring(base.length + 1)
      case _ => path
    }
  
  def cleanId(id: String): String = {
    val bytes = id.getBytes("UTF-8")
    val buffer = new StringBuffer()

    bytes.foreach { byte =>
      val i = (byte & 0xff).toInt
      if (
        i < 0x21   || i > 0x7e  || i == 0x22 || i == 0x2a || i == 0x2b ||
        i == 0x2c  || i == 0x3c || i == 0x3d || i == 0x3e || i == 0x3f ||
        i == 0x5c  || i == 0x5e || i == 0x7c
      ) {
        // Encode.
        buffer.append(HexIndicator)
        val hexString = Integer.toHexString(i)
        if (hexString.length < 2) buffer.append('0')
        buffer.append(hexString)
      } else {
        // Don't encode.
        val chars = Character.toChars(i)
        // This should never happen, so we'll treat it as actually exceptional.
        if (chars.length != 1) throw InvalidIdError(
          f"""Unexpected byte $byte%d in identifier "$id%s"."""
        )
        buffer.append(chars(0))
      }
    }

    buffer.toString.replace('/', '=').replace(':', '+').replace('.', ',')
  }
  
  def uncleanId(id: String): Throwable \/ String =
    \/.fromTryCatch {
      val buffer = scala.collection.mutable.ArrayBuffer.empty[Byte]
      var c = 0

      while (c < id.length) {
        id.charAt(c) match {
          case '=' => buffer ++= "/".getBytes("UTF-8")
          case '+' => buffer ++= ":".getBytes("UTF-8")
          case ',' => buffer ++= ".".getBytes("UTF-8")
          case '^' =>
            val hex = if (c + 3 > id.length) throw InvalidIdError(
              s"""Identifier "$id" ended unexpectedly."""
            ) else id.substring(c + 1, c + 3)

            val byte = try {
              Integer.parseInt(hex, 16).toByte
            } catch {
              case _: Throwable => throw InvalidIdError(
                s"""Expected "$hex" to be a hexadecimal integer."""
              )
            }

            buffer += byte
            c += 2

          case chr => buffer += chr.toByte
        }
        c += 1
      }

      new String(buffer.toArray, "UTF-8")
    }
}

