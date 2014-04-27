package edu.umd.mith.hathi

import edu.umd.mith.util.PairtreeParser

case class Record(id: String)
case class Htid(library: String, id: String) {
  override def toString() = s"$library.$id"
  def toFileName = s"$library.${ PairtreeParser.Default.cleanId(id) }"
  def idToFileName = PairtreeParser.Default.cleanId(id)
}

object Htid {
  def parse(s: String) = s.split("\\.").toList match {
    case List(id) => Htid("", id)
    case library :: idParts => Htid(library, idParts.mkString("."))
  }
  implicit val htidOrdering: Ordering[Htid] = Ordering.by {
    case Htid(library, id) => (library, id)
  }
}

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
