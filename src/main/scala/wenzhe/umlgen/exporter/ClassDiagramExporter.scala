package wenzhe.umlgen.exporter

import wenzhe.umlgen.plantuml.PlantUmlClassDiagramGenerator
import wenzhe.umlgen._

/**
 * @author wen-zhe.liu@asml.com
 *
 */
class ClassDiagramExporter(uml: ClassDiagramUml, conf: ExporterConfig) extends UmlExporter {
  def export(): Unit = {
    val generator = new PlantUmlClassDiagramGenerator(uml)
    println(s"Start to generate UML for ${conf.name}")
    val umlStr = generator.generate
    new UmlStringExporter(umlStr, conf).export()
  }
}
