package wenzhe.umlgen.exporter

import net.sourceforge.plantuml.SourceStringReader
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import wenzhe.umlgen.ImplicitHelper._
import java.io.IOException
import wenzhe.umlgen._
import java.nio.file.Path
import java.nio.file.Files

/**
 * @author wen-zhe.liu@asml.com
 *
 */
class UmlStringExporter(umlStr: String, conf: ExporterConfig) extends UmlExporter {
  def export(): Unit = {
    if (conf.printToConsole) println(umlStr)
    conf.exportToPlantUml foreach { wrapWithExportFileTips(_) {
      _ write umlStr
    }}
    val reader = new SourceStringReader(umlStr)
    Seq(FileFormat.PNG -> conf.exportToPNG, FileFormat.SVG -> conf.exportToSVG) foreach {
      case (fileFormat, Some(exportedPath)) => wrapWithExportFileTips(exportedPath) { exportedPath =>
          withResource (Files newOutputStream exportedPath) {
            try reader generateImage (_, new FileFormatOption(fileFormat))
            catch {
              case e: IOException => System.err.println(e.getMessage); null
            }
          }
        }
      case _ =>
    }
  }
  private def wrapWithExportFileTips(exportedPath: Path)(f: Path => Unit): Unit = {
    println(s"Exporting to $exportedPath ...")
    f(exportedPath)
    println(s"Exported to $exportedPath.")
  }
  
}