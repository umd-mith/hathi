package edu.umd.mith.hathi.api.data

import java.io.File
import scala.io.Source

/** Small utility trait to parse a .hathitrustrc configuration file containing
  * the user's consumer key and consumer secret. The file must contain exactly
  * two non-empty lines containing only the key and secret.
  */
trait DataClientConfig { this: DataClient =>
  /** We provide a sensible default; override as needed.
    */
  def configFile: File =
    new File(System.getProperty("user.home"), ".hathitrustrc")

  lazy val (key, secret) = {
    val source = Source.fromFile(configFile)
    source.getLines.map(_.trim).filter(_.nonEmpty).toList match {
      case List(k, s) => (k, s)
    }
  }
}
