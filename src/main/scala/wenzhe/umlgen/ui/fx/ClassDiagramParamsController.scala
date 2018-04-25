package wenzhe.umlgen.ui.fx

import java.net.URL
import java.util.ResourceBundle
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{CheckBox, ChoiceBox, Spinner, TextField}

import wenzhe.umlgen.project.ClassDiagramDataConfig
import wenzhe.umlgen.{ClassDiagramConfig, Modifier}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scalafx.beans.binding.BindingIncludes._
import scalafx.beans.property.BooleanProperty
import scalafx.event.subscriptions.Subscription
import scalafx.scene.control.ControlIncludes._
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory

class ClassDiagramParamsController extends Initializable {
  @FXML var enableCenterClass: CheckBox = _
  @FXML var centerClass: TextField = _
  @FXML var showField: CheckBox = _
  @FXML var showStaticField: CheckBox = _
  @FXML var sortField: CheckBox = _
  @FXML var showConstructor: CheckBox = _
  @FXML var showMethod: CheckBox = _
  @FXML var showStaticMethod: CheckBox = _
  @FXML var onlyShowMethodInInterface: CheckBox = _
  @FXML var sortMethod: CheckBox = _
  @FXML var showConstructorFirst: CheckBox = _
  @FXML var getterSetterToPublicField: CheckBox = _
  @FXML var showPackage: CheckBox = _
  @FXML var generateFullName: CheckBox = _
  @FXML var showOtherClassAsBase: CheckBox = _
  @FXML var leastFieldModifier: ChoiceBox[String] = _
  @FXML var leastMethodModifier: ChoiceBox[String] = _
  @FXML var maxDistanceToCenter: Spinner[Integer] = _
  @FXML var removeFieldIfRelateToOther: CheckBox = _
  @FXML var showExtension: CheckBox = _
  @FXML var showImplementation: CheckBox = _
  @FXML var showComposition: CheckBox = _
  @FXML var showAggregation: CheckBox = _
  @FXML var showDependencies: CheckBox = _
  @FXML var showDependencyToItself: CheckBox = _
  @FXML var removeDependencyIfExtend: CheckBox = _
  
  private var dirty: Option[BooleanProperty] = None
  val lock: BooleanProperty = BooleanProperty(false)
  private val subscriptions: ArrayBuffer[Subscription] = ArrayBuffer.empty  //TODO: dispose when close the tab

  def initialize(location: URL, resources: ResourceBundle) {
    showField.disable <== lock
    showStaticField.disable <== lock || !showField.selected
    sortField.disable <== lock || !showField.selected
    showMethod.disable <== lock
    showConstructor.disable <== lock || !showMethod.selected
    showStaticMethod.disable <== lock || !showMethod.selected
    onlyShowMethodInInterface.disable <== lock || !showMethod.selected
    sortMethod.disable <== lock || !showMethod.selected
    showConstructorFirst.disable <== lock || !showMethod.selected || !showConstructor.selected
    getterSetterToPublicField.disable <== lock || !showField.selected
    showPackage.disable <== lock
    generateFullName.disable <== lock
    showOtherClassAsBase.disable <== lock
    leastFieldModifier.disable <== lock || !showField.selected
    leastMethodModifier.disable <== lock || !showMethod.selected
    enableCenterClass.disable <== lock
    centerClass.disable <== lock || !enableCenterClass.selected
    maxDistanceToCenter.disable <== lock || !enableCenterClass.selected
    removeFieldIfRelateToOther.disable <== lock || !showField.selected
    showExtension.disable <== lock
    showImplementation.disable <== lock
    showComposition.disable <== lock
    showAggregation.disable <== lock
    showDependencies.disable <== lock
    showDependencyToItself.disable <== lock || !showDependencies.selected
    removeDependencyIfExtend.disable <== lock || !showDependencies.selected
    
    maxDistanceToCenter.valueFactory = new IntegerSpinnerValueFactory(0, 100, 1)
    leastFieldModifier.getSelectionModel select "Private"
    leastMethodModifier.getSelectionModel select "Protected"
    
    subscriptions ++= Seq(enableCenterClass.selected, centerClass.text, showField.selected, showStaticField.selected, 
        sortField.selected, showConstructor.selected, showMethod.selected, showStaticMethod.selected,
        onlyShowMethodInInterface.selected, sortMethod.selected, showConstructorFirst.selected,
        getterSetterToPublicField.selected, showPackage.selected, generateFullName.selected,
        showOtherClassAsBase.selected, leastFieldModifier.value, leastMethodModifier.value,
        maxDistanceToCenter.value, removeFieldIfRelateToOther.selected, showExtension.selected,
        showImplementation.selected, showComposition.selected, showAggregation.selected,
        showDependencies.selected, showDependencyToItself.selected, removeDependencyIfExtend.selected
        ).map { _.onChange {
      dirty.foreach { _.value = true }
    }}
  }
  def setDirty(v: BooleanProperty): Unit = {
    dirty = Some(v)
  }
  def classDiagramConfig(): ClassDiagramConfig = {
    val outer = ClassDiagramParamsController.this
    new ClassDiagramDataConfig(
      showField= outer.showField.isSelected(),
      leastFieldModifier = Modifier createByAccessorName outer.leastFieldModifier.getValue,
      showConstructor = outer.showConstructor.isSelected(),
      showMethod = outer.showMethod.isSelected(),
      onlyShowMethodInInterface = outer.onlyShowMethodInInterface.isSelected(),
      leastMethodModifier = Modifier createByAccessorName outer.leastMethodModifier.getValue,
      getterSetterToPublicField = outer.getterSetterToPublicField.isSelected(),
      showConstructorFirst = outer.showConstructorFirst.isSelected(),
      sortField = outer.sortField.isSelected(),
      sortMethod = outer.sortMethod.isSelected(),
      removeDependencyIfExtendOrAggregate = outer.removeDependencyIfExtend.isSelected(),
      removeFieldIfRelateToOther = outer.removeFieldIfRelateToOther.isSelected(),
      showDependencies = outer.showDependencies.isSelected(),
      showExtensions = outer.showExtension.isSelected(),
      showImplementations = outer.showImplementation.isSelected(),
      showAggregations = outer.showAggregation.isSelected(),
      showCompositions = outer.showComposition.isSelected(),
      showStaticField = outer.showStaticField.isSelected(),
      showStaticMethod = outer.showStaticMethod.isSelected(),
      showDependencyToSelf = outer.showDependencyToItself.isSelected(),
      showOtherClassAsBaseClass = outer.showOtherClassAsBase.isSelected(),
      generateFullName = outer.generateFullName.isSelected(),
      showPackage = outer.showPackage.isSelected(),
      centerClass = outer.centerClass.getText.trim(),
      maxDistanceToCenter = outer.maxDistanceToCenter.getValue
    )
  }
  def toXml(): Elem = {
    <classDiagram 
      enableCenterClass={enableCenterClass.isSelected().toString()}
      centerClass={centerClass.getText}
      showField={showField.isSelected().toString()}
      showStaticField={showStaticField.isSelected().toString()}
      sortField={sortField.isSelected().toString()}
      showConstructor={showConstructor.isSelected().toString()}
      showMethod={showMethod.isSelected().toString()}
      showStaticMethod={showStaticMethod.isSelected().toString()}
      onlyShowMethodInInterface={onlyShowMethodInInterface.isSelected().toString()}
      sortMethod={sortMethod.isSelected().toString()}
      showConstructorFirst={showConstructorFirst.isSelected().toString()}
      getterSetterToPublicField={getterSetterToPublicField.isSelected().toString()}
      showPackage={showPackage.isSelected().toString()}
      generateFullName={generateFullName.isSelected().toString()}
      showOtherClassAsBase={showOtherClassAsBase.isSelected().toString()}
      leastFieldModifier={leastFieldModifier.getValue}
      leastMethodModifier={leastMethodModifier.getValue}
      maxDistanceToCenter={maxDistanceToCenter.getValue.toString()}
      removeFieldIfRelateToOther={removeFieldIfRelateToOther.isSelected().toString()}
      showExtension={showExtension.isSelected().toString()}
      showImplementation={showImplementation.isSelected().toString()}
      showComposition={showComposition.isSelected().toString()}
      showAggregation={showAggregation.isSelected().toString()}
      showDependencies={showDependencies.isSelected().toString()}
      showDependencyToItself={showDependencyToItself.isSelected().toString()}
      removeDependencyIfExtend={removeDependencyIfExtend.isSelected().toString()}
      ></classDiagram>
  }
  def load(node: scala.xml.NodeSeq) = {
    enableCenterClass setSelected (node \@ "enableCenterClass" toBoolean)
    centerClass setText (node \@ "centerClass")
    showField setSelected(node \@ "showField" toBoolean)
    showStaticField setSelected(node \@ "showStaticField" toBoolean)
    sortField setSelected(node \@ "sortField" toBoolean)
    showConstructor setSelected(node \@ "showConstructor" toBoolean)
    showMethod setSelected(node \@ "showMethod" toBoolean)
    showStaticMethod setSelected(node \@ "showStaticMethod" toBoolean)
    onlyShowMethodInInterface setSelected(node \@ "onlyShowMethodInInterface" toBoolean)
    sortMethod setSelected(node \@ "sortMethod" toBoolean)
    showConstructorFirst setSelected(node \@ "showConstructorFirst" toBoolean)
    getterSetterToPublicField setSelected(node \@ "getterSetterToPublicField" toBoolean)
    showPackage setSelected(node \@ "showPackage" toBoolean)
    generateFullName setSelected(node \@ "generateFullName" toBoolean)
    showOtherClassAsBase setSelected(node \@ "showOtherClassAsBase" toBoolean)
    leastFieldModifier setValue(node \@ "leastFieldModifier")
    maxDistanceToCenter.getValueFactory setValue(node \@ "maxDistanceToCenter" toInt)
    removeFieldIfRelateToOther setSelected(node \@ "removeFieldIfRelateToOther" toBoolean)
    showExtension setSelected(node \@ "showExtension" toBoolean)
    showImplementation setSelected(node \@ "showImplementation" toBoolean)
    showComposition setSelected(node \@ "showComposition" toBoolean)
    showAggregation setSelected(node \@ "showAggregation" toBoolean)
    showDependencies setSelected(node \@ "showDependencies" toBoolean)
    showDependencyToItself setSelected(node \@ "showDependencyToItself" toBoolean)
    removeDependencyIfExtend setSelected(node \@ "removeDependencyIfExtend" toBoolean)
  }
}