package wenzhe.umlgen.ui.fx

import java.io.File
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.ResourceBundle
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.GridPane
import javafx.stage.Window

import wenzhe.umlgen.ExporterConfig
import wenzhe.umlgen.ImplicitHelper._
import wenzhe.umlgen.project._

import scala.concurrent.Future
import scala.sys.process._
import scala.util.Properties
import scala.xml.Elem
import scalafx.application.Platform
import scalafx.beans.property.PropertyIncludes._
import scalafx.collections.CollectionIncludes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ControlIncludes._
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.stage.DirectoryChooser

/**
  * Created by weliu on 6/6/2017.
  */
class ProductClassDiagramController extends TabController with Initializable {

  @FXML var supportJava: CheckBox = _
  @FXML var supportScala: CheckBox = _
  @FXML var exportToPlantUmlSource: CheckBox = _
  @FXML var exportPng: CheckBox = _
  @FXML var exportSvg: CheckBox = _
  @FXML var rootPathToExport: TextField = _
  @FXML var btnBrowseExportPath: Button = _
  @FXML var btnGoToExportPath: Button = _
  @FXML var btnAddDirectory: Button = _
  @FXML var btnDelete: Button = _
  @FXML var productTree: TreeView[Path] = _
  @FXML var classDiagramParams: GridPane = _
  @FXML var classDiagramParamsController: ClassDiagramParamsController = _

  @FXML var imgAddDir: ImageView = _
  @FXML var imgDelete: ImageView = _
  @FXML var imgGo: ImageView = _

  val typeName = "Product Class Diagram"

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    supportJava.disable <== lock
    supportScala.disable <== lock
    exportToPlantUmlSource.disable <== lock
    exportPng.disable <== lock
    exportSvg.disable <== lock
    rootPathToExport.disable <== lock
    btnAddDirectory.disable <== lock
    btnDelete.disable <== lock
    productTree.disable <== lock
    btnBrowseExportPath.disable <== lock
    classDiagramParamsController.lock <== lock

    subscriptions ++= Seq(supportJava.selected, supportScala.selected,
      exportToPlantUmlSource.selected, exportPng.selected, exportSvg.selected,
      rootPathToExport.text, productTree.root
    ).map(_.onChange {
      dirty.value = true
    })

    subscriptions ++= Seq(productTree.rootProperty).map(_.onChange {
      dirty.value = true
    })
//    Seq(productTree.getItems) foreach { _ onChange {
//      dirty.value = true
//    }}
    classDiagramParamsController setDirty dirty

    btnBrowseExportPath.onAction = { _ =>
      val fileChooser = new DirectoryChooser {
        title = "Select a directory to export"
        Some(rootPathToExport.getText.trim).filterNot(_.isEmpty).map(new File(_))
        .orElse(lastBrowseDir).filter(_.isDirectory).foreach {
          initialDirectory = _
        }
      }
      val selectedFolder = fileChooser.showDialog(btnBrowseExportPath.getScene.getWindow)
      if (selectedFolder != null) {
        lastBrowseDir = Some(selectedFolder)
        rootPathToExport setText selectedFolder.getPath
      }
    }
    btnGoToExportPath.onAction = { _ =>
      goToExportedFolder()
    }

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
    productTree.cellFactory = { _ => new TreeCell[Path]() {
      override def updateItem(item: Path, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          val relative = Option(getTreeItem.getParent).map(_.getValue).map(_.relativize(item)).getOrElse(item)
          setText(Option(relative.toString).filterNot(_.isEmpty).getOrElse("."))
          setTooltip(new Tooltip(item.toString))
        }
      }
    }}

    imgAddDir.setImage(new Image(getClass getResourceAsStream "/icon/folder_new_16x16.png"))
    imgGo.setImage(new Image(getClass getResourceAsStream "/icon/review.png"))
    imgDelete.setImage(new Image(getClass getResourceAsStream "/icon/remove_16x16.png"))
  }
  private def goToExportedFolder(): Unit = {
    val filePath = rootPathToExport.getText.trim
    val cmd = Properties.envOrNone("FILE_VIEWER").map(_ + s" '$filePath'")
    if (cmd.isDefined) cmd.get run new ProcessIO({_=>}, {_=>}, {_=>}, true)
    else {
      if (Properties.isWin) {
        openFirstAvailableApp(filePath, Seq("explorer"))
      } else if (Properties.isLinux) {
        s"gnome-terminal --working-directory ${filePath}" run new ProcessIO({_=>}, {_=>}, {_=>}, true)
      }
    }
  }

  @FXML def onAddDirectory() {
    val fileChooser = new DirectoryChooser {
      title = "Select a directory to analysis"
      lastBrowseDir.filter(_.isDirectory).foreach {
        initialDirectory = _
      }
    }
    val selectedFile = fileChooser.showDialog(productTree.getScene.getWindow)
    selectFile(selectedFile)
  }
  private def setProduct(path: Path): TreeItem[Path] = {
    val root = new TreeItem[Path](path)
    root.setExpanded(true)
    productTree.root = root
    subscriptions += productTree.getRoot.getChildren.onChange {
      dirty.value = true
    }
    productTree.getRoot
  }
  private def addProject(path: Path): TreeItem[Path] = {
    val projectItem = new TreeItem[Path](path)
    projectItem.setExpanded(true)
    productTree.getRoot.getChildren.add(projectItem)
    productTree.getSelectionModel.select(projectItem)
    subscriptions += projectItem.getChildren.onChange {
      dirty.value = true
    }
    projectItem
  }
  private def addSourceRoot(parent: TreeItem[Path], path: Path): TreeItem[Path] = {
    val srcItem = new TreeItem[Path](path)
    parent.getChildren.add(srcItem)
    parent.setExpanded(true)
    productTree.getSelectionModel.select(srcItem)
    srcItem
  }
  private def selectFile(selectedFile: File) {
    if (selectedFile == null) return
    lastBrowseDir = Some(selectedFile)
    if (productTree.getRoot == null) {
      setProduct(selectedFile.toPath)
    } else {
      val selectedItem = productTree.getSelectionModel.getSelectedItem
      if (selectedItem == null || selectedItem == productTree.getRoot) {
        val selectedPath = selectedFile.toPath
        val projectItem = addProject(selectedPath)

        // add default "src/main/java" and "src/main/scala"
        var defaultSrcDirs: Traversable[Path] = Seq("java" -> supportJava, "scala" -> supportScala)
          .filter(_._2.isSelected).map(_._1)
          .map(selectedPath / "src" / "main" / _)
          .filter(_ exists)
        if (defaultSrcDirs.isEmpty) {
          defaultSrcDirs = Option(selectedPath / "src").filter(_ exists).orElse(Some(selectedPath))
        }
        defaultSrcDirs.foreach {
          addSourceRoot(projectItem, _)
        }
      } else if (selectedItem.getParent == productTree.getRoot) {
        addSourceRoot(selectedItem, selectedFile.toPath)
      }
    }
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
        productTree.getSelectionModel.getSelectedItems.forEach{ item =>
          val parent = item.getParent
          if (parent == null) productTree.setRoot(null)
          else parent.getChildren.remove(item)
        }
      case _ =>
    }
  }

  def productConf(): ProductConfig = {
    val isExportSvg = exportSvg.isSelected
    val isExportUml = exportToPlantUmlSource.isSelected
    val isExportPng = exportPng.isSelected
    val exportRootPath: Path = Paths.get(rootPathToExport.getText.trim)
    val productName = productTree.getRoot.getValue.getFileName.toString
    val productFilterConfig = new FilterDataConfig(
      recursiveLevel = Int.MaxValue,
      supportedLanguages = Map(
        "java" -> supportJava, "scala" -> supportScala
      ).filter(_._2.isSelected()).keySet
    )
    val productClassDiagramConfig = classDiagramParamsController.classDiagramConfig
    new ProductDataConfig(
      filterConfig = productFilterConfig,
      exporterConfig = new ExporterDataConfig(
        name = productName,
        exportToSVG = if (exportSvg.isSelected && !exportRootPath.toString.isEmpty) Some(
          exportRootPath / (productName + ".product.svg")
        ) else None,
        exportToPlantUml = if (exportToPlantUmlSource.isSelected && !exportRootPath.toString.isEmpty) Some(
          exportRootPath / (productName + ".product.uml")
        ) else None,
        exportToPNG = if (exportPng.isSelected && !exportRootPath.toString.isEmpty) Some(
          exportRootPath / (productName + ".product.png")
        ) else None,
        printToConsole = false
      ),
      classDiagramConfig = productClassDiagramConfig,
      projects = productTree.getRoot.getChildren.map { treeItem =>
        val projectName: String = treeItem.getValue.getFileName.toString
        val exportPrjRootPath: Path = exportRootPath / projectName
        new ProjectDataConfig (
          srcRootDirs = treeItem.getChildren.map(_.getValue),
          exporterConfig = new ExporterDataConfig (
            exportToSVG = if (exportSvg.isSelected && !exportRootPath.toString.isEmpty) Some(
              exportPrjRootPath / (projectName + ".project.svg")
            ) else None,
            exportToPlantUml = if (exportToPlantUmlSource.isSelected && !exportRootPath.toString.isEmpty) Some(
              exportPrjRootPath / (projectName + ".project.uml")
            ) else None,
            name = treeItem.getValue.getFileName.toString,
            exportToPNG = if (exportPng.isSelected && !exportRootPath.toString.isEmpty) Some(
              exportPrjRootPath / (projectName + ".project.png")
            ) else None,
            printToConsole = false
          ),
          filterConfig= productFilterConfig,
          classDiagramConfig = productClassDiagramConfig,
          packageConf = new PackageDataConfig(
            filterConfig = productFilterConfig,
            classDiagramConfig = productClassDiagramConfig
          ) {
            def exporterConfig(pkgName: String): ExporterConfig = new ExporterDataConfig (
              name = pkgName,
              exportToSVG = if (isExportSvg && !exportRootPath.toString.isEmpty) Some(
                exportPrjRootPath / (pkgName + ".svg")
              ) else None,
              exportToPlantUml = if (isExportUml && !exportRootPath.toString.isEmpty) Some(
                exportPrjRootPath / (pkgName + ".uml")
              ) else None,
              exportToPNG = if (isExportPng && !exportRootPath.toString.isEmpty) Some(
                exportPrjRootPath / (pkgName + ".png")
              ) else None,
              printToConsole = false
            )
          }
        )
      }
    )
  }

  override def run(): Unit = {
    val productCfg = productConf()
    lock.value = true
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      new ProductUml(productCfg).toExporter.export()
    } onComplete { _ =>
      Platform.runLater {
        lock.value = false
        goToExportedFolder()
      }
    }
  }
  protected def window: Window = productTree.getScene.getWindow

  protected def toXml(): Elem = {
    <productClassDiagram
    supportJava={supportJava.isSelected().toString()}
    supportScala={supportScala.isSelected().toString()}
    exportToPlantUmlSource={exportToPlantUmlSource.isSelected().toString()}
    exportPng={exportPng.isSelected().toString()}
    exportSvg={exportSvg.isSelected().toString()}
    rootPathToExport={rootPathToExport.getText.trim}
    >
      {classDiagramParamsController.toXml()}
      {Option(productTree.getRoot).map(product =>
      <product path={product.getValue.toString}>
        {product.getChildren.map(project =>
        <project path={project.getValue.toString}>
          {project.getChildren.map(src =>
          <src>{src.getValue.toString}</src>
        )}
        </project>
      )}
      </product>).toSeq}
    </productClassDiagram>
  }
  def load(node: scala.xml.Node): Unit = {
    supportJava setSelected (node \@ "supportJava" toBoolean)
    supportScala setSelected (node \@ "supportScala" toBoolean)
    exportToPlantUmlSource setSelected (node \@ "exportToPlantUmlSource" toBoolean)
    exportPng setSelected (node \@ "exportPng" toBoolean)
    exportSvg setSelected (node \@ "exportSvg" toBoolean)
    rootPathToExport setText (node \@ "rootPathToExport")
    classDiagramParamsController.load(node \ "classDiagram")
    for (product <- node \ "product") {
      setProduct(Paths get (product \@ "path"))
      for (project <- product \ "project") {
        val projectItem = addProject(Paths get (project \@ "path"))
        for (src <- project \ "src") {
          addSourceRoot(projectItem, Paths get src.text)
        }
      }
    }
  }
}
