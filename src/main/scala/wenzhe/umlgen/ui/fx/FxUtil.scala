package wenzhe.umlgen.ui.fx

import java.io.File

import scalafx.scene.{control => sfxc}
import scalafx.scene.{image => sfxi}
import javafx.scene.Node
import java.util.function.UnaryOperator
import javafx.scene.control.TextFormatter.Change
import javafx.scene.control.TextFormatter

import scalafx.beans.property.{BooleanProperty, StringProperty}


object FxUtil {
  def intTextFormatter: TextFormatter[String] = new TextFormatter({ change: Change =>
    val text = change.getText
    if (text.matches("[0-9]*")) change else null
  })
  def createFileExistenceProperty(filePath: StringProperty): BooleanProperty = {
    val exist = BooleanProperty(false)
    filePath.onInvalidate {
      exist.value = new File(filePath.value.trim).exists
    }
    exist
  }
}
