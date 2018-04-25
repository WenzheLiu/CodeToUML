package wenzhe.umlgen.ui.fx

import java.io.File
import java.nio.file.{Path, Paths}
import javafx.stage.Window

import scala.collection.mutable.ArrayBuffer
import scala.sys.process.ProcessIO
import scala.xml.{Elem, Node, XML}
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.event.subscriptions.Subscription
import scalafx.stage.FileChooser
import scalafx.collections.CollectionIncludes._
import scala.sys.process._
import wenzhe.umlgen.ImplicitHelper._

abstract class TabController {
  val title: StringProperty = StringProperty("Code to UML")
  val dirty: BooleanProperty = BooleanProperty(false)
  val lock: BooleanProperty = BooleanProperty(false)
  val tabText: StringProperty = StringProperty("")
  val tabTooltip: StringProperty = StringProperty("")
  val sessionFile: ObjectProperty[Either[String, File]] = ObjectProperty(Left(""))
  val subscriptions: ArrayBuffer[Subscription] = ArrayBuffer.empty
  var showConsole: () => Unit = () => {}
  protected var lastBrowseDir: Option[File] = None
  protected def toXml(): Elem

  def run(): Unit
  def load(node: Node): Unit
  protected def window: Window

  def save(): Unit = {
    val file = sessionFile.value
    if (file.isLeft) saveAs()
    else save(file.right.get)
  }
  def saveAs(): Unit = {
    val fileChooser = new FileChooser {
      title = "Select a file to save"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("XML Files", "*.xml"),
        new FileChooser.ExtensionFilter("All Files", "*.*")
      )
      lastBrowseDir.filter(_.isDirectory).foreach {
        initialDirectory = _
      }
    }
    val selectedFile = fileChooser.showSaveDialog(window)
    if (selectedFile != null) {
      save(selectedFile)
      sessionFile.value = Right(selectedFile)
      lastBrowseDir = Some(selectedFile)
    }
  }
  private def save(file: File): Unit = {
    //val pp = new PrettyPrinter(80, 4)
    //file.toPath write pp.format(toXml)
    XML.save(file.toString(), toXml, "UTF-8", true)
    dirty.value = false
  }

  def sessionPath: Option[Path] = {
    sessionFile.value.toOption.map(_.toPath)
  }

  protected def openFirstAvailableApp(filePath: String, apps: Traversable[String]) = {
    apps find (Paths get _ exists) foreach {
      Seq(_, filePath) run new ProcessIO({ _ => }, { _ => }, { _ => }, true)
    }
  }
}
