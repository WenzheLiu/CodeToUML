package wenzhe.umlgen

import java.nio.file.Path
import java.nio.file.Files

import scala.collection.JavaConversions._
import scala.util.Properties

trait FileFilter {
  def filter(files: Traversable[Path]): Traversable[Path]
}

class RecursableFileFilter(isAccept: Path => Boolean, recursiveLevel: Int = Int.MaxValue) extends FileFilter {
  def filter(files: Traversable[Path]): Traversable[Path] = {
    filterRecursively(files, recursiveLevel)
  }
  private def filterRecursively(files: Traversable[Path], recursiveLevel: Int): Traversable[Path] = {
    files collect {
      case folder: Path if Files.isDirectory(folder) && recursiveLevel > 0 => 
        filterRecursively(Files.list(folder).iterator.toStream, recursiveLevel - 1)
      case file: Path if isAccept(file) => Stream(file)
    } flatten
  }
}
