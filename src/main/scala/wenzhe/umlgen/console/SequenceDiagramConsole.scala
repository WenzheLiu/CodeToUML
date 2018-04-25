package wenzhe.umlgen.console

import wenzhe.umlgen._
import scala.util.Properties
import java.nio.file.Paths
import wenzhe.umlgen.plantuml.SequenceDiagramGenerator
import scala.io.StdIn
import wenzhe.umlgen.exporter.UmlStringExporter
import wenzhe.umlgen.config.ExporterPropConfig
import wenzhe.umlgen.seqdiagram.SeqDiagramParser

/**
 * @author wen-zhe.liu@asml.com
 * 
 * For example: java -Dxxx=xxx -jar xxx.jar C:\Users\weliu\.code2uml\1234\54968\1
 * -Duml.package=wenzhe.umlgen 
 * -Duml.class=ClassRelationFactory 
 * -Duml.method=createRelations 
 * -Dapp.exportToPlantUml=C:\Users\weliu\code\sbt\tmp\seq.uml 
 * -Dapp.exportToPNG=C:\Users\weliu\code\sbt\tmp\seq.png 
 * -Dapp.exportToSVG=C:\Users\weliu\code\sbt\tmp\seq.svg
 * -Dapp.printToConsole=true
 *
 */
object SequenceDiagramConsole {
  def main(args: Array[String]) {
    if (args isEmpty) {
      System.err println "Argument should not be empty"
      System exit -1
    }
    val files = args map (Paths get _)
    val conf = new ExporterPropConfig
    val pkg = Properties.propOrEmpty("uml.package")
    val cls = Properties.propOrEmpty("uml.class")
    val method = Properties.propOrEmpty("uml.method")
    val inputMethod = SeqMethod(SeqClass(pkg, cls), method)
    
    val umls = SeqDiagramParser.parse(inputMethod, files).map(new SequenceDiagramGenerator(_)).map(_.generate)
    println(s"There are ${umls.size} sequence diagrams, which one would you choose? (1 - ${umls.size})")
    val selectedUmlId = StdIn.readInt()
    val uml = umls(selectedUmlId - 1)
    new UmlStringExporter(uml, conf).export()
  }
}
