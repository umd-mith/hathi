package edu.umd.mith.hathi

import edu.umd.mith.hathi.mets.MetsFile
import edu.umd.mith.util.{ DefaultPairtreeSystem, FileUtils, ZipFileUtils }
import java.io.File
import scalaz._, Scalaz._

/** Represents a single HathiTrust volume item in the data set.
  */
case class DatasetItem(metsFile: File, zipFile: File)

object Dataset {
  case class DatasetError(msg: String) extends Exception(msg)
}

/** Represents a HathiTrust data set serialized as a set of Pairtree systems
  * organized by library.
  */
class Dataset(val base: File) extends FileUtils with ZipFileUtils {
  def item(htid: Htid): Throwable \/ DatasetItem =
    isDir(new File(new File(base, htid.library), "pairtree_root")).flatMap { libraryRootDir =>
      val system = new DefaultPairtreeSystem(libraryRootDir)

      system.directory(htid.id).flatMap { dir =>
        isDir(new File(dir, htid.idToFileName)).flatMap { itemBaseDir =>
          for {
            metsFile <- isFile(new File(itemBaseDir, s"${ htid.idToFileName }.mets.xml"))
            zipFile <- isFile(new File(itemBaseDir, s"${ htid.idToFileName }.zip"))
          } yield DatasetItem(metsFile, zipFile)
        }
      }
    }

  def pages(htid: Htid): Throwable \/ List[Page] = item(htid).flatMap {
    case DatasetItem(metsFile, zipFile) =>
      for {
        mets <- MetsFile.fromFile(metsFile)
        files <- zipFileContents(zipFile).get.run.map(_.toMap)
        pages <- mets.pageMetadata.traverseU { pageMetadata =>
          val path = s"${ htid.idToFileName }/${ pageMetadata.textPath }"
          files.get(path).toRightDisjunction(
            Dataset.DatasetError(s"Missing page at $path.")
          ).map(contents => Page(pageMetadata, contents))
        }
      } yield pages
  }
}
