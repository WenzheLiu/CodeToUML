package wenzhe.umlgen.factory

import wenzhe.umlgen._
import java.nio.file.Path
import wenzhe.umlgen.javaparser.JavaUmlFactory
import wenzhe.umlgen.scalaparser.ScalaUmlFactory

class FilterableUmlFactory(supportedLanguages: Set[String], recursiveLevel: Int = Int.MaxValue) extends UmlFactory {
  def this(conf: FilterConfig) = this(conf.supportedLanguages, conf.recursiveLevel)
  def createClassDiagramUml(files: Traversable[Path]): ClassDiagramUml = {
    val umls = Seq("java" -> JavaUmlFactory, "scala" -> ScalaUmlFactory) filter {
      case (lang, _) => supportedLanguages contains lang
    } map {
      case (lang, umlFactory) => 
        val fileFilter = new RecursableFileFilter(_.toString.endsWith("." + lang), recursiveLevel)
        umlFactory createClassDiagramUml (fileFilter filter files)
    }
    new ComposableClassDiagram(umls)
  }
}
