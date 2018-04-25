package wenzhe.umlgen

import java.nio.file.{Path, Files}
import scala.collection.JavaConversions._
import scala.util.parsing.json.JSON

object ImplicitHelper {
  implicit class PathHelper(path: Path) {
    def /(subPath: String) = path resolve subPath
    def /(subPath: Path) = path resolve subPath
    def mkdirs() = Files createDirectories path
    def createFile() = {
       Files createDirectories (path getParent)
       Files createFile path
    }
    def exists() = Files exists path
    def write(content: String) = {
      if (!exists) createFile
      Files write (path, content getBytes)
    }
    def writeIfNotExist(content: String) = {
        if (!exists) {
          createFile
          Files write (path, content getBytes)
        }
    }
    def ->:(content: String) = write(content)
    def content: String = Files readAllLines path mkString "\n"
  }
  def withResource[R <: AutoCloseable, T](resource: R)(f: R => T): T = {
    try f(resource)
    finally resource.close()
  }
  def parseJson(file: Path): Option[Map[String, Any]] = JSON parseFull (file content) collect {
    case m: Map[_, _] => m collect {
      case (k: String, v: Any) => (k, v)
    }
  }
}