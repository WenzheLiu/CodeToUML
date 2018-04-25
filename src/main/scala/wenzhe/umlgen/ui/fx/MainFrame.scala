package wenzhe.umlgen.ui.fx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.image.Image
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

import com.sun.javafx.application.LauncherImpl

import scalafx.stage.StageIncludes._

object MainFrameRun {
  def main(args: Array[String]) {
    LauncherImpl.launchApplication(classOf[MainFrame], args)
  }
}

class MainFrame extends Application {
  override def start(stage: Stage) {
    val resource = getClass getResource "/fxml/Frame.fxml"
    val loader = new FXMLLoader(resource)
    val root: Parent = loader.load()
    val controller: FrameController = loader.getController.asInstanceOf[FrameController]
    val scene = new Scene(root)
    stage setScene scene
    stage setTitle "Code To UML"
    stage.title <== controller.title
    stage.onCloseRequest = { _ =>
      controller.saveOpenedSessions()
    }
    stage.getIcons.add(new Image(this.getClass.getResourceAsStream("/icon/lovely_bird_32px.png")))
    stage.show()
  }
}