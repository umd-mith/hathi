package edu.umd.mith.hathi
package mets

import edu.umd.mith.util.PairtreeParser
import java.io.{ File, FileReader, FileWriter, Reader, StringReader }
import scala.language.postfixOps
import scales.utils._
import scales.xml._, ScalesXml._, xpath.AttributePath
import scalaz._, Scalaz._

/** Helpers for constructing METS file representations.
  */
object MetsFile {
  def fromString(contents: String) = fromReader(new StringReader(contents))
  def fromFile(file: File) = fromReader(new FileReader(file))

  def fromReader(reader: Reader): Throwable \/ MetsFile = {
    \/.fromTryCatch(loadXml(reader)).flatMap { doc =>
      val parser = new MetsFileParser(doc)

      for {
        htid <- parser.parseHtid
        fileMap <- parser.parseFileMap
        zipFileName <- parser.parseZipFileName
        pageMetadata <- parser.parsePageMetadata
      } yield MetsFile(
        doc,
        htid,
        fileMap,
        zipFileName,
        pageMetadata
      )
    }
  }
}

/** Represents a successfully parsed METS file.
  */
case class MetsFile(
  doc: Doc,
  htid: Htid,
  fileMap: Map[String, (String, String)],
  zipFileName: String,
  pageMetadata: List[PageMetadata]
) {
  def save(output: File): Unit = {
    val writer = new FileWriter(output)
    writeTo(doc, writer)
    writer.close()
  }
}

/** Parses METS files, providing access to metadata about pages, etc.
  */
class MetsFileParser(val doc: Doc) extends MetsModel {
  private[this] val metsNs = Namespace("http://www.loc.gov/METS/")
  private[this] val xlinkNs = Namespace("http://www.w3.org/1999/xlink")

  def error(msg: String): Throwable = parseHtid.swap.valueOr(htid =>
    UnexpectedMetsStructure(htid, msg)
  )

  def attrOpt[PT <: Iterable[XmlPath]](path: AttributePaths[PT]): Option[String] =
    path.one.headOption.map(_.attribute.value)

  def attr[PT <: Iterable[XmlPath]](path: AttributePaths[PT])
    (msg: String): Throwable \/ String =
    path.one.headOption.toRightDisjunction(error(msg)).map(_.attribute.value)

  def elemWithAttr[T <: Iterable[XmlPath]](x: XPath[T])
    (attrAndValue: (String, String)): Throwable \/ XmlPath =
    attrAndValue match {
      case (attr, value) =>
        (x \@ attr === value).\^.one.headOption.toRightDisjunction(
          error(s"""Expected attribute "$attr" with value "$value".""")
        )
    }

  def parseLabels(labels: Option[String]): Throwable \/ Set[String] =
    labels.fold[Throwable \/ Set[String]](Set.empty.right) { labelString =>
      val labels = labelString.split(",\\s*").toSet
      val unknownLabels = labels - "" -- knownLabels

      unknownLabels.nonEmpty.either(
        error(s"Unknown labels: ${unknownLabels.mkString(", ")}.")
      ).or(labels)
    }

  lazy val parseHtid: Throwable \/ Htid =
    (top(doc) \@ "OBJID").one.headOption.toRightDisjunction(
      MetsError("Missing object identifier (OBJID).")
    ).map(attr => Htid.parse(attr.attribute.value))

  lazy val parseFileMap: Throwable \/ Map[String, (String, String)] = (
    top(doc) \* metsNs("fileSec") \* metsNs("fileGrp") \* metsNs("file")
  ).toList.traverseU { file =>
    val id = attr(file \@ "ID")("Missing ID.")
    val href = attr(file \* metsNs("FLocat") \@ xlinkNs("href"))("Missing link.")
    val mimetype = attr(file \@ "MIMETYPE")("Missing mimetype.")

    id.tuple(href.tuple(mimetype))
  }.map(_.toMap)

  lazy val parseZipFileName: Throwable \/ String = elemWithAttr(
    top(doc) \* metsNs("fileSec") \* metsNs("fileGrp")
  )("USE" -> "zip archive").flatMap { fileGrp =>
    attr(fileGrp \* metsNs("file") \* metsNs("FLocat") \@ xlinkNs("href"))(
      "Expected file link for zip archive."
    )
  }

  lazy val parsePageMetadata: Throwable \/ List[PageMetadata] = for {
    htid <- parseHtid
    fileMap <- parseFileMap
    zipFileName <- parseZipFileName

    structMap <- (top(doc) \* metsNs("structMap")).one.headOption.toRightDisjunction(
      error("Expected one structure map.")
    )

    volumeDiv <- elemWithAttr(structMap \* metsNs("div"))("TYPE" -> "volume")

    pages <- (volumeDiv \* metsNs("div") \@ "TYPE" === "page" \^).toList.traverseU { page =>
      for {
        order <- attr(page \@ "ORDER")("Missing order attribute.")
        orderLabel = attrOpt(page \@ "ORDERLABEL")

        labels <- parseLabels(attrOpt(page \@ "LABEL"))

        pageTextPath <- (page \* metsNs("fptr") \@ "FILEID").toList.map(_.attribute.value).filter(
          fileId => fileId.startsWith("TXT") || fileId.startsWith("OCR")
        ).headOption.toRightDisjunction(
          error("Missing text file identifier.")
        ).flatMap(fileId =>
          fileMap.get(fileId).map(_._1).toRightDisjunction(
            error(s"""Missing path for "$fileId".""")
          )
        )

        image <- (page \* metsNs("fptr") \@ "FILEID").toList.map(
          _.attribute.value
        ).filter(
          fileId => fileId.startsWith("IMG")
        ).headOption.toRightDisjunction(
          error("Missing image file identifier.")
        ).flatMap(fileId =>
          fileMap.get(fileId).toRightDisjunction(
            error(s"""Missing path for "$fileId".""")
          )
        )

        ocr <- (page \* metsNs("fptr") \@ "FILEID").toList.map(
          _.attribute.value
        ).filter(
          fileId => fileId.startsWith("HTML") || fileId.startsWith("XML")
        ).headOption.toRightDisjunction(
          error("Missing OCR file identifier.")
        ).flatMap(fileId =>
          fileMap.get(fileId).toRightDisjunction(
            error(s"""Missing path for "$fileId".""")
          )
        )
      } yield new PageMetadata {
        val textPath = pageTextPath
        val imagePath = image._1
        val imageMimeType = image._2
        val ocrPath = ocr._1
        val ocrMimeType = ocr._2
        val seq = order
        val number = orderLabel
        val allLabels = labels
        val contents = ""
      }
    }
  } yield pages
}
