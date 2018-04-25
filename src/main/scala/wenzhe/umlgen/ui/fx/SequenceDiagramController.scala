package wenzhe.umlgen.ui.fx

import java.net.URL
import java.nio.file.Path
import java.util.ResourceBundle
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{Button, CheckBox, ListView, TextField}
import javafx.scene.image.ImageView
import javafx.stage.Window

import scala.xml.{Elem, Node}

/**
  * Created by weliu on 6/27/2017.
  */
class SequenceDiagramController extends TabController with Initializable {

  @FXML var btnAddFile: Button = _
  @FXML var btnDelete: Button = _
  @FXML var analysisFiles: ListView[Path] = _
  @FXML var textClass: TextField = _
  @FXML var textMethod: TextField = _
  @FXML var btnAnalysis: TextField = _
  @FXML var exportToPlantUmlSource: CheckBox = _
  @FXML var umlSourceFile: TextField = _
  @FXML var btnBrowseUml: Button = _
  @FXML var btnOpenUml: Button = _
  @FXML var exportPng: CheckBox = _
  @FXML var pngFile: TextField = _
  @FXML var btnBrowsePng: Button = _
  @FXML var btnOpenPng: Button = _
  @FXML var exportSvg: CheckBox = _
  @FXML var svgFile: TextField = _
  @FXML var btnBrowseSvg: Button = _
  @FXML var btnOpenSvg: Button = _

  @FXML var imgAddFile: ImageView = _
  @FXML var imgAddDir: ImageView = _
  @FXML var imgDelete: ImageView = _
  @FXML var imgOpenUml: ImageView = _
  @FXML var imgOpenPng: ImageView = _
  @FXML var imgOpenSvg: ImageView = _

  override protected def toXml(): Elem = ???

  override def run(): Unit = ???

  override def load(node: Node): Unit = ???

  override protected def window: Window = ???

  override def initialize(location: URL, resources: ResourceBundle): Unit = ???
}
