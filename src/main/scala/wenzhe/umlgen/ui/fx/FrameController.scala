package wenzhe.umlgen.ui.fx

import java.io.{File, OutputStream, PrintStream}
import java.net.URL
import java.nio.file.Paths
import java.util.ResourceBundle
import javafx.fxml.{FXML, FXMLLoader, Initializable}
import javafx.scene.Node
import javafx.scene.control.{Button, TabPane, TextArea, TitledPane}
import javafx.scene.image.{Image, ImageView}

import wenzhe.umlgen.ImplicitHelper._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.util.{Properties => SProperties}
import scala.xml.XML
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.event.subscriptions.Subscription
import scalafx.scene.SceneIncludes._
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.{control => sfxc, image => sfxi}
import scalafx.stage.FileChooser

class FrameController extends Initializable {

  @FXML var tabPane: TabPane = _
  @FXML var outputPane: TitledPane = _
  @FXML var console: TextArea = _
  @FXML var btnSave: Button = _
  @FXML var btnRun: Button = _
  @FXML var btnStop: Button = _

  @FXML var imgCreateClassUml: ImageView = _
  @FXML var imgCreateProductUml: ImageView = _
  @FXML var imgOpen: ImageView = _
  @FXML var imgSave: ImageView = _
  @FXML var imgSaveAs: ImageView = _
  @FXML var imgRun: ImageView = _
  @FXML var imgStop: ImageView = _
  @FXML var menuImgNew: ImageView = _
  @FXML var menuImgOpen: ImageView = _
  @FXML var menuImgSave: ImageView = _
  @FXML var menuImgSaveAs: ImageView = _

  val sessionsFile = Paths get (SProperties.userHome, ".code2uml", "sessions.xml")
  val subscriptions: ArrayBuffer[Subscription] = ArrayBuffer.empty

  val title = StringProperty("Code To UML")
  
  override def initialize(location: URL, resources: ResourceBundle) {
    subscriptions += tabPane.getSelectionModel.selectedItem.onChange { (_, _, tab) =>
      Option(tab).map(_.getUserData).collect {
        case controller: TabController =>
          btnSave.disable <== !controller.dirty
          btnRun.disable <== controller.lock
          btnStop.disable <== !controller.lock
          title <== controller.title
      }
    }
    val out = new OutputStream {
      def write(b: Int): Unit = Platform.runLater {
        console.appendText(b.toChar.toString)
      }
    }
    System.setOut(new PrintStream(out))

    imgCreateClassUml.setImage(new Image(getClass getResourceAsStream "/icon/new.png"))
    imgCreateProductUml.setImage(new Image(getClass getResourceAsStream "/icon/new_product.png"))
    imgOpen.setImage(new Image(getClass getResourceAsStream "/icon/open.png"))
    imgSave.setImage(new Image(getClass getResourceAsStream "/icon/save.png"))
    imgSaveAs.setImage(new Image(getClass getResourceAsStream "/icon/saveas.png"))
    imgRun.setImage(new Image(getClass getResourceAsStream "/icon/run.png"))
    imgStop.setImage(new Image(getClass getResourceAsStream "/icon/stop.png"))
    menuImgNew.setImage(new Image(getClass getResourceAsStream "/icon/new.png"))
    menuImgOpen.setImage(new Image(getClass getResourceAsStream "/icon/open.png"))
    menuImgSave.setImage(new Image(getClass getResourceAsStream "/icon/save.png"))
    menuImgSaveAs.setImage(new Image(getClass getResourceAsStream "/icon/saveas.png"))

    loadOpenedSessions()
  }

  @FXML def createStandaloneClassDiagram() {
    val untitled = "Untitled"
    var i = 1
    while (tabPane.getTabs.map(_.getText).contains(untitled + i)) i = i + 1

    createStandaloneClassDiagram(Left(untitled + i))
  }
  @FXML def createProductClassDiagram(): Unit = {
    val untitled = "Untitled"
    var i = 1
    while (tabPane.getTabs.map(_.getText).contains(untitled + i)) i = i + 1

    createProductClassDiagram(Left(untitled + i))
  }
  private def createSessionTab(fxmlPath: String, iconPath: String)(file: Either[String, File]): TabController = {
    val resource = getClass getResource fxmlPath
    val loader = new FXMLLoader(resource)
    val root: Node = loader.load()
    val controller = loader.getController.asInstanceOf[TabController]
    controller.sessionFile.value = file
    controller.showConsole = showConsole

    val tab = new sfxc.Tab {
      text <== controller.tabText
      graphic = new sfxi.ImageView {
        image = new sfxi.Image(getClass getResourceAsStream iconPath)
      }
      tooltip = new sfxc.Tooltip {
        text <== controller.tabTooltip
      }
      content = root
      userData = controller
    }

    tabPane.getTabs add tab
    tabPane.getSelectionModel selectLast

    controller
  }
  private def createStandaloneClassDiagram(file: Either[String, File]): TabController = {
    createSessionTab("/fxml/StandaloneClassDiagram.fxml", "/icon/lovely_cat_32px.png")(file)
  }
  private def createProductClassDiagram(file: Either[String, File]): TabController = {
    createSessionTab("/fxml/ProductClassDiagram.fxml", "/icon/lovely_32px.png")(file)
  }
  private def selectedTabController(): Option[TabController] = {
    val item = tabPane.getSelectionModel.getSelectedItem
    Option(item.getUserData).collect {
      case c: TabController => c
    }
  }
  @FXML def onAbout(): Unit = {
    new Alert(AlertType.Information) {
      title = "About"
      headerText = "Convert Java/Scala code to UML"
      contentText =
        """
          |Author: Wenzhe Liu
          |
          |Contact: liuwenzhe2008@qq.com
        """.stripMargin
    }.showAndWait()
  }
  private def showConsole(): Unit = outputPane.setExpanded(true)
  @FXML def run(): Unit = selectedTabController.foreach {
    showConsole
    _.run
  }
  @FXML def save(): Unit = selectedTabController.foreach(_.save)
  @FXML def saveAs(): Unit = selectedTabController.foreach(_.saveAs)
  @FXML def open() {
    val fileChooser = new FileChooser {
       title = "Select a file to open"
       extensionFilters ++= Seq(
         new FileChooser.ExtensionFilter("XML Files", "*.xml"),
         new FileChooser.ExtensionFilter("All Files", "*.*")
       )
    }
    val selectedFile = fileChooser.showOpenDialog(tabPane.getScene.getWindow)
    if (selectedFile != null) {
      open(selectedFile)
    }
  }

  private def open(selectedFile: File): Unit = {
    val node: xml.Node = XML.loadFile(selectedFile)
    val controllerFactories: Map[String, Either[String, File] => TabController] = Map(
      "standaloneClassDiagram" -> {input => createStandaloneClassDiagram(input)},
      "productClassDiagram" -> {input => createProductClassDiagram(input)}
    )
    controllerFactories.find(node.label == _._1).map(_._2)
      .map(_(Right(selectedFile)))
      .foreach(controller => try controller load node catch {
        case ex: Exception => ex.printStackTrace()
      } finally controller.dirty.value = false)
  }

  def saveOpenedSessions(): Unit = {
    val sessionPaths = tabPane.tabs.map(_.getUserData).collect {
      case controller: TabController => controller.sessionPath
    }.collect {
      case Some(sessionPath) => sessionPath.toString
    }
    val xml = <sessions>
      {sessionPaths.map(path => <session id={path}/>)}
    </sessions>
    sessionsFile.getParent.mkdirs
    XML.save(sessionsFile.toString, xml, "UTF-8", true)
  }
  private def loadOpenedSessions(): Unit = if (sessionsFile exists) {
    val root = XML loadFile sessionsFile.toFile
    root \ "session" map (_ \@ "id") map (new File(_)) foreach open
  }
}
