package edu.umd.mith.util.marc

import java.io.ByteArrayInputStream
import org.marc4j.MarcXmlReader
import org.marc4j.marc.{ DataField, Record }
import org.marc4j.marc.impl.{ DataFieldImpl, SubfieldImpl }
import scala.collection.JavaConverters._
import scalaz.concurrent.Task

case class MarcDataField(
  indicator1: Char,
  indicator2: Char,
  subfields: Map[Char, String]
)

trait MarcRecord {
  def underlying: Record

  def controlFields: Map[String, String]
  def dataFields: Map[String, List[MarcDataField]]

  def subjects: List[String] = dataFields.getOrElse("650", Nil).flatMap(
    _.subfields.values.toList
  )

  def authors: List[String] = dataFields.getOrElse("100", Nil).flatMap(
    _.subfields.values.toList
  )

  def secondaryAuthors: List[String] = dataFields.getOrElse("700", Nil).flatMap(
    _.subfields.values.toList
  )

  def titles: List[String] = dataFields.getOrElse("245", Nil).flatMap(
    _.subfields.values.toList
  )

  def editions: List[String] = dataFields.getOrElse("250", Nil).flatMap(
    _.subfields.values.toList
  )

  def addOrReplace(tag: String, indicator1: Char, indicator2: Char, subfieldCode: Char)
    (data: String): Unit =
    Option(underlying.getVariableField(tag)).fold {
      underlying.addVariableField {
        val df = new DataFieldImpl(tag, indicator1, indicator2)
        df.addSubfield(new SubfieldImpl(subfieldCode, data))
        df
      }
    } {
      case df: DataField =>
        Option(df.getSubfield(subfieldCode)).fold(
          df.addSubfield(new SubfieldImpl(subfieldCode, data))
        )(_.setData(data))
    }

  def addOrModify(tag: String, indicator1: Char, indicator2: Char, subfieldCode: Char)
    (data: String, f: String => String): Unit =
    Option(underlying.getVariableField(tag)).fold {
      underlying.addVariableField {
        val df = new DataFieldImpl(tag, indicator1, indicator2)
        df.addSubfield(new SubfieldImpl(subfieldCode, data))
        df
      }
    } {
      case df: DataField =>
        Option(df.getSubfield(subfieldCode)).fold(
          df.addSubfield(new SubfieldImpl(subfieldCode, data))
        )(sf => sf.setData(f(sf.getData)))
    }

  def add(tag: String, indicator1: Char, indicator2: Char, subfieldCode: Char)
    (data: String): Unit =
    underlying.addVariableField {
      val df = new DataFieldImpl(tag, indicator1, indicator2)
      df.addSubfield(new SubfieldImpl(subfieldCode, data))
      df
    }

  def modify(tag: String, subfieldCode: Char)(f: String => String): Unit =
    underlying.getVariableFields(tag).asScala.foreach {
      case df: DataField =>
        Option(df.getSubfield(subfieldCode)).foreach(
          sf => sf.setData(f(sf.getData))
        )
    }
}

class MarcRecordWrapper(val underlying: Record) extends MarcRecord {
  val controlFields = underlying.getControlFields.asScala.map(
    field => field.getTag -> field.getData
  ).toMap

  val dataFields = underlying.getDataFields.asScala.groupBy(
    _.getTag
  ).mapValues(
    _.map(field =>
      MarcDataField(
        field.getIndicator1,
        field.getIndicator2,
        field.getSubfields.asScala.map {
          subfield => subfield.getCode -> subfield.getData
        }.toMap
      )
    ).toList
  ).view.force
}

trait MarcUtils {
  def parseMarcXmlString(xmlString: String): Task[List[MarcRecord]] =
    Task.delay {
      val stream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"))
      val records = scala.collection.mutable.ListBuffer.empty[MarcRecord]

      val reader = new MarcXmlReader(stream) 

      while (reader.hasNext()) {
        new MarcRecordWrapper(reader.next()) +=: records
      }

      records.reverse.toList
    }
}
