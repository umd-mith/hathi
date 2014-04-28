package edu.umd.mith.hathi

import argonaut._, Argonaut._
import edu.umd.mith.util.ArgonautUtils
import edu.umd.mith.util.marc.{ MarcUtils, MarcRecord }
import java.net.URL
/*import org.apache.commons.lang.StringEscapeUtils.{
  escapeJavaScript,
  unescapeJavaScript
}*/
import scalaz._, Scalaz._

/** Provides JSON codecs for metadata types.
  */
trait MetadataJson extends ArgonautUtils with MarcUtils {
  def parseVolumeMetadata(htid: Htid)(jsonString: String): Throwable \/ VolumeMetadata =
    Parse.parse(jsonString).flatMap(
      _.as(decodeVolumeMetadata(htid)).toDisjunction.leftMap(_._1)
    ).leftMap(ArgonautUtils.ArgonautError(_))

  def decodeVolumeMetadata(htid: Htid): DecodeJson[VolumeMetadata] = DecodeJson(c =>
    (c --\ "items").downArray.find(
      _.obj.flatMap(_.toMap.get("htid").flatMap(_.string)).map(
        _ == htid.toString
      ).getOrElse(false)
    ).as[VolumeMetadata]
  )

  implicit val UpdatedCodecJson: CodecJson[Updated] = CodecJson(
    (a: Updated) => Json.jString(a.toString),
    (c: HCursor) => c.as[String].flatMap {
      case "00000000" => DecodeResult.ok(Updated(None))
      case s => disjunctionToResult(c.history)(
        Updated.parseLocalDate(s)
      ).map(date => Updated(Some(date)))
    }
  )

  /*implicit val EscapedCodecJson: CodecJson[Escaped] = CodecJson(
    (a: Escaped) => Json.jString(escapeJavaScript(a.value)),
    (c: HCursor) => c.as[String].map(s => Escaped(unescapeJavaScript(s)))
  )*/

  implicit val EnumcronEncodeJson: CodecJson[Enumcron] = CodecJson.derived(
    EncodeJson[Enumcron] {
      case BooleanEnumcron(value) => Json.jBool(value)
      case OtherEnumcron(value) => Json.jString(value)
    },
    implicitly[DecodeJson[Boolean]].map[Enumcron](BooleanEnumcron(_)) |||
    implicitly[DecodeJson[String]].map(OtherEnumcron(_))
  )

  implicit val MarcRecordListDecodeJson: DecodeJson[List[MarcRecord]] = DecodeJson(c =>
    c.as[String].flatMap(xmlString => taskResult(c.history)(parseMarcXmlString(xmlString)))
  )

  implicit val VolumeMetadataDecodeJson: DecodeJson[VolumeMetadata] = DecodeJson(c =>
    for {
      url <- (c --\ "itemURL").as[URL]
      htid <- (c --\ "htid").as[String]
      orig <- (c --\ "orig").as[String]
      fromRecord <- (c --\ "fromRecord").as[String]
      record <- (c.up.up --\ "records" --\ fromRecord).as[RecordMetadata]
      rightsCode <- (c --\ "rightsCode").as[String]
      lastUpdate <- (c --\ "lastUpdate").as[Updated]
      enumcron <- (c --\ "enumcron").as[Enumcron].flatMap {
        case BooleanEnumcron(true) => DecodeResult.fail("Enumcron should never be true.", c.history)
        case BooleanEnumcron(false) => DecodeResult.ok(None)
        case OtherEnumcron(s) => DecodeResult.ok(Some(s))
      }
      usRights <- (c --\ "usRightsString").as[String]
    } yield VolumeMetadata(
      url,
      Htid.parse(htid),
      orig,
      record,
      rightsCode,
      lastUpdate,
      enumcron,
      usRights
    )
  )

  implicit val RecordMetadataDecodeJson: DecodeJson[RecordMetadata] = DecodeJson(c =>
    for {
      recordId <- (c --\ "recordURL").as[String].map(RecordId(_))
      url <- (c --\ "recordURL").as[java.net.URL]
      titles <- (c --\ "titles").as[List[String]]
      isbns <- (c --\ "isbns").as[List[String]]
      issns <- (c --\ "issns").as[List[String]]
      oclcs <- (c --\ "oclcs").as[List[String]]
      publishDates <- (c --\ "publishDates").as[List[String]]
      marcRecord <- (c --\ "marc-xml").as[List[MarcRecord]].flatMap {
        case List(rec) => DecodeResult.ok(rec)
        case _ => DecodeResult.fail("Too many MARC records for item.", c.history)
      }
    } yield RecordMetadata(
      recordId,
      url,
      titles,
      isbns,
      issns,
      oclcs,
      publishDates,
      marcRecord
    )
  )
}

