package edu.umd.mith.hathi

import edu.umd.mith.util.PairtreeParser
import edu.umd.mith.util.marc.MarcRecord
import java.net.URL
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import scalaz.{ Ordering => _, _ }, Scalaz._

/** Represents a HathiTrust record identifier.
  */
case class RecordId(id: String)

/** Represents a HathiTrust volume identifier.
  */
case class Htid(library: String, id: String) {
  override def toString() = s"$library.$id"
  def toFileName = s"$library.${ PairtreeParser.Default.cleanId(id) }"
  def idToFileName = PairtreeParser.Default.cleanId(id)
}

/** Utilities and instances for HathiTrust volume identifiers.
  */
object Htid {
  def parse(s: String) = s.split("\\.").toList match {
    case List(id) => Htid("", id)
    case library :: idParts => Htid(library, idParts.mkString("."))
  }
  implicit val htidOrdering: Ordering[Htid] = Ordering.by {
    case Htid(library, id) => (library, id)
  }
}

/** Represents metadata for a single page.
  */
trait PageMetadata {
  def textPath: String
  def imagePath: String
  def imageMimeType: String
  def ocrPath: String
  def ocrMimeType: String
  def seq: String
  def number: Option[String]
  def allLabels: Set[String]

  def isMultiworkBoundary = allLabels("MULTIWORK_BOUNDARY")
  def isFrontCover = allLabels("FRONT_COVER") || allLabels("COVER")
  def isTitle = allLabels("TITLE")
  def isCopyright = allLabels("COPYRIGHT")
  def isFirstContentChapterStart = allLabels("FIRST_CONTENT_CHAPTER_START")
  def isChapterStart = allLabels("CHAPTER_START")
  def isReferences = allLabels("REFERENCES")
  def isIndex = allLabels("INDEX")
  def isBackCover = allLabels("BACK_COVER")
  def isBlank = allLabels("BLANK")
  def isUntypical = allLabels("UNTYPICAL_PAGE")
  def hasImage = allLabels("IMAGE_ON_PAGE")
  def hasImplicitNumber = allLabels("IMPLICIT_PAGE_NUMBER")

  override def toString: String =
    s"""$textPath: $seq${ number.fold("")(n => " (" + n + ")") }"""
}

/** Represents metadata for a HathiTrust record.
  */
case class RecordMetadata(
  id: RecordId,
  url: URL,
  titles: List[String],
  isbns: List[String],
  issns: List[String],
  oclcs: List[String],
  publishDates: List[String],
  marc: MarcRecord
)

/** Represents metadata for a HathiTrust volume.
  */
case class VolumeMetadata(
  url: URL,
  htid: Htid,
  orig: String,
  record: RecordMetadata,
  rights: String,
  lastUpdate: Updated,
  enumcron: Option[String],
  usRights: String
)

/** Represents a date that a volume was updated.
  */
case class Updated(date: Option[LocalDate]) {
  override def toString = date.fold("00000000")(Updated.format.print)
}

/** Data parsing utilities.
  */
object Updated {
  val format = DateTimeFormat.forPattern("yyyyMMdd")

  def parseLocalDate(s: String): Throwable \/ LocalDate =
    \/.fromTryCatch(format.parseLocalDate(s))
}

/** Represents an identifier for a volume in a record.
  */
sealed trait Enumcron
case class BooleanEnumcron(value: Boolean) extends Enumcron
case class OtherEnumcron(value: String) extends Enumcron
