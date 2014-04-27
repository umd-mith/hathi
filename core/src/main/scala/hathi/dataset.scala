package edu.umd.mith.hathi

import edu.umd.mith.util.{ DefaultPairtreeSystem, FileUtils }
import java.io.File
import scalaz._, Scalaz._

/** Represents a single HathiTrust volume item in the data set.
  */
case class DatasetItem(metsFile: File, zipFile: File)

/** Represents a HathiTrust data set serialized as a set of Pairtree systems
  * organized by library.
  */
class Dataset(val base: File) extends FileUtils {
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
}
