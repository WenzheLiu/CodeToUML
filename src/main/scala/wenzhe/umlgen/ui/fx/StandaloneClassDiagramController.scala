package wenzhe.umlgen.ui.fx

import java.io.File
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.ResourceBundle
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{Button, _}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.GridPane
import javafx.stage.Window

import wenzhe.umlgen._
import wenzhe.umlgen.ImplicitHelper._
import wenzhe.umlgen.config.ConfigurableClassDiagramUml
import wenzhe.umlgen.exporter.{ClassDiagramExporter, UmlStringExporter}
import wenzhe.umlgen.factory.FilterableUmlFactory

import scala.concurrent.Future
import scala.sys.process._
import scala.util.Properties
import scala.xml.Elem
import scalafx.application.Platform
import scalafx.collections.CollectionIncludes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ControlIncludes._
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control.{Alert, ButtonType, SelectionMode}
import scalafx.stage.{DirectoryChooser, FileChooser}

/**
 * @author wen-zhe.liu@asml.com
 *
 */
class StandaloneClassDiagramController extends TabController with Initializable {
  @FXML var supportJava: CheckBox = _
  @FXML var supportScala: CheckBox = _
  @FXML var limitRecursiveLevel: CheckBox = _
  @FXML var recursiveLevel: Spinner[Integer] = _
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
  @FXML var btnAddDirectory: Button = _
  @FXML var btnAddFile: Button = _
  @FXML var btnDelete: Button = _
  @FXML var analysisFiles: ListView[Path] = _
  @FXML var classDiagramParams: GridPane = _
  @FXML var classDiagramParamsController: ClassDiagramParamsController = _

  @FXML var imgAddFile: ImageView = _
  @FXML var imgAddDir: ImageView = _
  @FXML var imgDelete: ImageView = _
  @FXML var imgOpenUml: ImageView = _
  @FXML var imgOpenPng: ImageView = _
  @FXML var imgOpenSvg: ImageView = _


  val typeName = "Standalone Class Diagram"

  def initialize(location: URL, resources: ResourceBundle) {
    analysisFiles.getSelectionModel.selectionMode = SelectionMode.Multiple

    recursiveLevel.valueFactory = new IntegerSpinnerValueFactory(0, 1000, 1)

    supportJava.disable <== lock
    supportScala.disable <== lock
    limitRecursiveLevel.disable <== lock
    recursiveLevel.disable <== lock || !limitRecursiveLevel.selected

    exportToPlantUmlSource.disable <== lock
    umlSourceFile.disable <== lock || !exportToPlantUmlSource.selected
    btnBrowseUml.disable <== lock || !exportToPlantUmlSource.selected

    exportPng.disable <== lock
    pngFile.disable <== lock || !exportPng.selected
    btnBrowsePng.disable <== lock || !exportPng.selected

    exportSvg.disable <== lock
    svgFile.disable <== lock || !exportSvg.selected
    btnBrowseSvg.disable <== lock || !exportSvg.selected
    btnAddDirectory.disable <== lock
    btnAddFile.disable <== lock
    btnDelete.disable <== lock

    classDiagramParamsController.lock <== lock

    subscriptions ++= Seq(supportJava.selected, supportScala.selected, limitRecursiveLevel.selected, recursiveLevel.value,
        exportToPlantUmlSource.selected, umlSourceFile.text, exportPng.selected, pngFile.text,
        exportSvg.selected, svgFile.text
        ).map { _.onChange {
          dirty.value = true
        }
    }
    subscriptions ++= Seq(analysisFiles.getItems) map { _ onChange {
      dirty.value = true
    }}
    classDiagramParamsController setDirty dirty

    subscriptions ++= List(sessionFile, dirty).map { _.onInvalidate {
      val file = sessionFile.value
      val isDirty = dirty.value
      val dirtyPostfix = if (isDirty) " *" else ""
      if (file.isLeft) {
        tabText.value = file.left.get + dirtyPostfix
        tabTooltip.value = typeName
        title.value = file.left.get + dirtyPostfix + " - " + typeName
      } else {
        val ext = ".xml"
        val filePath = file.right.get
        val name = filePath.getName
        val nameWithoutExt = if (name endsWith ext) name dropRight ext.length else name

        tabText.value = nameWithoutExt + dirtyPostfix
        tabTooltip.value = typeName + "\n" + filePath.toString
        title.value = filePath.toString + dirtyPostfix + " - " + typeName
      }
    }}

    imgAddDir.setImage(new Image(getClass getResourceAsStream "/icon/folder_new_16x16.png"))
    imgAddFile.setImage(new Image(getClass getResourceAsStream "/icon/add_16x16.png"))
    imgDelete.setImage(new Image(getClass getResourceAsStream "/icon/remove_16x16.png"))
    imgOpenUml.setImage(new Image(getClass getResourceAsStream "/icon/review.png"))
    imgOpenPng.setImage(new Image(getClass getResourceAsStream "/icon/review.png"))
    imgOpenSvg.setImage(new Image(getClass getResourceAsStream "/icon/review.png"))
  }

  @FXML def onAddFile() {
    val fileChooser = new FileChooser {
      title = "Select a file to analysis"
      lastBrowseDir.filter(_.isDirectory).foreach {
        initialDirectory = _
      }
    }
    val selectedFile = fileChooser.showOpenDialog(analysisFiles.getScene.getWindow)
    selectFile(selectedFile)
  }

  @FXML def onAddDirectory() {
    val fileChooser = new DirectoryChooser {
      title = "Select a directory to analysis"
      lastBrowseDir.filter(_.isDirectory).foreach {
        initialDirectory = _
      }
    }
    val selectedFile = fileChooser.showDialog(analysisFiles.getScene.getWindow)
    selectFile(selectedFile)
  }

  private def selectFile(selectedFile: File): Unit = if (selectedFile != null) {
    analysisFiles.getItems.add(selectedFile.toPath())
    analysisFiles.getSelectionModel.selectLast()
    lastBrowseDir = Some(selectedFile)
  }

  @FXML def onRemoveSelectedFiles() {
    val alert = new Alert(AlertType.Confirmation) {
      title = "Remove the selected items"
      headerText = "Do you want to remove the selected items?"
      contentText = "Choose your option."
      buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
    }
    val result = alert.showAndWait()
    result match {
      case Some(ButtonType.OK) =>
        analysisFiles.getItems.removeAll(analysisFiles.getSelectionModel.getSelectedItems)
      case _ =>
    }
  }

  def filterConfig(): FilterConfig = new FilterConfig {
    val outer = StandaloneClassDiagramController.this
    val recursiveLevel: Int = if (limitRecursiveLevel.isSelected) outer.recursiveLevel.getValue else Int.MaxValue
    val supportedLanguages: Set[String] = Map(
      "java" -> outer.supportJava, "scala" -> outer.supportScala
    ).filter(_._2.isSelected()).keySet
  }
  def exporterConfig(): ExporterConfig = new ExporterConfig {
    private val outer = StandaloneClassDiagramController.this
    val name: String = outer.tabText.value
    val exportToPlantUml: Option[Path] = if (!outer.exportToPlantUmlSource.isSelected()) None else {
      val file = outer.umlSourceFile.getText
      if (file.isEmpty) None else Some(Paths get file)
    }
    val exportToPNG: Option[Path] = if (!outer.exportPng.isSelected()) None else {
      val file = outer.pngFile.getText
      if (file.isEmpty) None else Some(Paths get file)
    }
    val exportToSVG: Option[Path] = if (!outer.exportSvg.isSelected()) None else {
      val file = outer.svgFile.getText
      if (file.isEmpty) None else Some(Paths get file)
    }
    val printToConsole: Boolean = false
  }

  def run(): Unit = {
    lock.value = true
    val files: Traversable[Path] = analysisFiles.getItems.toTraversable
    val classDiagramConfig = classDiagramParamsController.classDiagramConfig
    val filterCfg = filterConfig
    val exporterCfg = exporterConfig
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val umlFac = new FilterableUmlFactory(filterCfg)
      val uml = new ConfigurableClassDiagramUml(classDiagramConfig,
        umlFac createClassDiagramUml files)
      val exporter = new ClassDiagramExporter(uml, exporterCfg)
      exporter.export()
    } onComplete { _ =>
      Platform.runLater {
        lock.value = false
        if (exportPng.isSelected) openPngImage()
        else if (exportSvg.isSelected) openSvgFile()
        else new Alert(AlertType.Information) {
          title = "Information Dialog"
          headerText = "Successfully exported"
        }.showAndWait()
      }
    }
  }
  protected def toXml(): Elem = {
    <standaloneClassDiagram
      supportJava={supportJava.isSelected().toString()}
      supportScala={supportScala.isSelected().toString()}
      limitRecursiveLevel={limitRecursiveLevel.isSelected().toString()}
      recursiveLevel={recursiveLevel.getValue.toString()}
      exportToPlantUmlSource={exportToPlantUmlSource.isSelected().toString()}
      umlSourceFile={umlSourceFile.getText}
      exportPng={exportPng.isSelected().toString()}
      pngFile={pngFile.getText}
      exportSvg={exportSvg.isSelected().toString()}
      svgFile={svgFile.getText}
      >
      {analysisFiles.getItems.map(_.toString).map(file => <analysisFile>{file}</analysisFile>)}
      {classDiagramParamsController.toXml()}
    </standaloneClassDiagram>
  }

  def load(node: scala.xml.Node): Unit = {
    supportJava setSelected (node \@ "supportJava" toBoolean)
    supportScala setSelected (node \@ "supportScala" toBoolean)
    limitRecursiveLevel setSelected(node \@ "limitRecursiveLevel" toBoolean)
    recursiveLevel.getValueFactory setValue (node \@ "recursiveLevel" toInt)
    exportToPlantUmlSource setSelected (node \@ "exportToPlantUmlSource" toBoolean)
    umlSourceFile setText (node \@ "umlSourceFile")
    exportPng setSelected (node \@ "exportPng" toBoolean)
    pngFile setText (node \@ "pngFile")
    exportSvg setSelected (node \@ "exportSvg" toBoolean)
    svgFile setText (node \@ "svgFile")
    for (item <- node \ "analysisFile") {
      analysisFiles.getItems.add(Paths get item.text)
    }
    classDiagramParamsController.load(node \ "classDiagram")
  }

  @FXML def browseUmlFileToExport(): Unit = browseFile(
    umlSourceFile, "Select a file path to export as Plant UML source code",
    new FileChooser.ExtensionFilter("Text Files", Seq("*.txt", "*.uml"))
  )

  @FXML def openUmlFile(): Unit = if (new File(umlSourceFile.getText.trim).exists) {
    val filePath = umlSourceFile.getText.trim
    val cmd = Properties.envOrNone("TEXT_VIEWER").map(_ + s" '$filePath'")
    if (cmd.isDefined) cmd.get run new ProcessIO({_=>}, {_=>}, {_=>}, true)
    else {
      if (Properties.isWin) {
        openFirstAvailableApp(filePath, Seq(
            Properties.envOrElse("ProgramFiles", "C:\\Program Files")
              + "\\Windows NT\\Accessories\\wordpad.exe"
          ))
      } else if (Properties.isLinux) {
        openFirstAvailableApp(filePath, Seq("/usr/bin/gvim"))
      }
    }
  }

  @FXML def browsePngFileToExport(): Unit = browseFile(
    pngFile, "Select a PNG image file path to export",
    new FileChooser.ExtensionFilter("PNG Files", "*.png")
  )

  private def browseFile(textField: TextField, browserTitle: String,
                         extensionFilter: FileChooser.ExtensionFilter): Unit = {
    val fileChooser = new FileChooser {
      title = browserTitle
      extensionFilters ++= Seq(
        extensionFilter,
        new FileChooser.ExtensionFilter("All Files", "*.*")
      )
      Some(textField.getText.trim).filterNot(_.isEmpty).map(new File(_).getParentFile)
        .orElse(lastBrowseDir).filter(_.isDirectory).foreach {
        initialDirectory = _
      }
    }
    val selectedFile = fileChooser.showSaveDialog(analysisFiles.getScene.getWindow)
    if (selectedFile != null) {
      textField.setText(selectedFile.toString)
      lastBrowseDir = Some(selectedFile.getParentFile)
    }
  }

  private def updatePngIfNeed(): Unit = {
    val pngFilePath = new File(pngFile.getText.trim)
    updateImageIfNeed(pngFilePath, new ExporterConfig {
      val name: String = ""
      val exportToPlantUml: Option[Path] = None
      val exportToPNG: Option[Path] = Some(pngFilePath.toPath)
      val exportToSVG: Option[Path] = None
      val printToConsole: Boolean = false
    })
  }

  private def updateSvgIfNeed(): Unit = {
    val svgFilePath = new File(svgFile.getText.trim)
    updateImageIfNeed(svgFilePath, new ExporterConfig {
      val name: String = ""
      val exportToPlantUml: Option[Path] = None
      val exportToPNG: Option[Path] = None
      val exportToSVG: Option[Path] = Some(svgFilePath.toPath)
      val printToConsole: Boolean = false
    })
  }

  private def updateImageIfNeed(imgFilePath: File, exporterConfig: ExporterConfig): Unit = {
    val umlFilePath = new File(umlSourceFile.getText.trim)
    if (umlFilePath.isFile) {
      if (!imgFilePath.isFile || umlFilePath.lastModified > imgFilePath.lastModified) {
        showConsole
        new UmlStringExporter(umlFilePath.toPath.content, exporterConfig).export()
      }
    }
  }

  @FXML def openPngImage(): Unit = if (!pngFile.getText.trim.isEmpty) {
    updatePngIfNeed
    val filePath = pngFile.getText
    val cmd = Properties.envOrNone("PNG_VIEWER").map(_ + s" '$filePath'")
    if (cmd.isDefined) cmd.get run new ProcessIO({_=>}, {_=>}, {_=>}, true)
    else {
      if (Properties.isWin) {
        Seq("C:\\Windows\\System32\\rundll32.exe",
          Properties.envOrElse("ProgramFiles", "C:\\Program Files") + "\\Windows Photo Viewer\\PhotoViewer.dll",
          "ImageView_Fullscreen", filePath) run new ProcessIO({ _ => }, { _ => }, { _ => }, true)
      } else if (Properties.isLinux) {
        openFirstAvailableApp(filePath, Seq("/usr/bin/firefox"))
      }
    }
  }

  @FXML def browseSvgFileToExport(): Unit = browseFile(
    svgFile, "Select a SVG file path to export",
    new FileChooser.ExtensionFilter("SVG Files", "*.svg")
  )

  @FXML def openSvgFile(): Unit = if (!svgFile.getText.trim.isEmpty) {
    updateSvgIfNeed
    val filePath = svgFile.getText
    val cmd = Properties.envOrNone("SVG_VIEWER").map(_ + s" '$filePath'")
    if (cmd.isDefined) cmd.get run new ProcessIO({_=>}, {_=>}, {_=>}, true)
    else {
      if (Properties.isWin) {
        openFirstAvailableApp(filePath, Seq(
          "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
          "C:\\Program Files (x86)\\Internet Explorer\\iexplore.exe"
        ))
      } else if (Properties.isLinux) {
        openFirstAvailableApp(filePath, Seq("/usr/bin/firefox"))
      }
    }
  }

  protected def window: Window = analysisFiles.getScene.getWindow
}