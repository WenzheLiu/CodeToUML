package wenzhe.umlgen.config

import java.nio.file.Path
import java.nio.file.Paths
import scala.util.Properties
import java.util.{Properties => JProperties}
import java.nio.file.Files
import wenzhe.umlgen._

/**
 * @author wen-zhe.liu@asml.com
 *
 */

class UmlPropertyConfig extends AppConfig {
  val filterConfig: FilterConfig = new FilterPropConfig
  val exporterConfig: ExporterConfig = new ExporterPropConfig
  val classDiagramConfig: ClassDiagramConfig = new ClassDiagramPropConfig
}

object PropParser {
  private lazy val userProps: JProperties = {
    val prop = new JProperties()
    Properties propOrNone "uml.propFile" map (Paths get _) orElse {
      val confFile = Paths get (Properties.userHome, ".code2uml", "config.properties")
      if (Files isRegularFile confFile) Some(confFile) else None
    } foreach { path =>
      val reader = Files newBufferedReader path
      try prop load reader
      finally reader.close
    }
    prop
  }
  
  def prop[T](name: String)(implicit toT: String => T): Option[T] = {
    Properties propOrNone name orElse {
      Option(userProps getProperty name)
    } orElse {
      Properties scalaPropOrNone name
    } map (_.trim) map {
      toT(_)
    }
  }
  implicit val toBoolean: String => Boolean = List("true", "yes", "on") contains _.toLowerCase
  implicit val toInt: String => Int = Integer parseInt _
  implicit val toModifier: String => Modifier = Modifier createByAccessorName _
}
import PropParser._

class FilterPropConfig extends FilterConfig {
  val recursiveLevel: Int = prop[Int]("app.recursiveLevel") filter (_ >= 0) getOrElse Int.MaxValue
  val supportedLanguages: Set[String] = prop[String]("app.supportedLanguages") map {
    _ split (";") toSet
  } getOrElse Set("java;scala") map (_ trim) map (_ toLowerCase)
}
class ExporterPropConfig extends ExporterConfig {
  val name: String = prop[String]("app.name") getOrElse "Input files"
  val exportToPlantUml: Option[Path] = prop[String]("app.exportToPlantUml") filterNot (_ isEmpty) map {
    Paths get _ toAbsolutePath
  }
  val exportToPNG: Option[Path] = prop[String]("app.exportToPNG") filterNot (_ isEmpty) map {
    Paths get _ toAbsolutePath
  }
  val exportToSVG: Option[Path] = prop[String]("app.exportToSVG") filterNot (_ isEmpty) map {
    Paths get _ toAbsolutePath
  }
  val printToConsole: Boolean = prop[Boolean]("app.printToConsole") getOrElse false
}
class ClassDiagramPropConfig extends ClassDiagramConfig {
  val showField: Boolean = prop[Boolean]("classdiagram.showField") getOrElse true
  val leastFieldModifier: Modifier = prop[Modifier]("classdiagram.leastFieldModifier") getOrElse {
    SimpleModifier(Accessor.PRIVATE)
  }
  val showConstructor: Boolean = prop[Boolean]("classdiagram.showConstructor") getOrElse true
  val showMethod: Boolean = prop[Boolean]("classdiagram.showMethod") getOrElse true
  val onlyShowMethodInInterface: Boolean = prop[Boolean]("classdiagram.onlyShowMethodInInterface") getOrElse false
  val leastMethodModifier: Modifier = prop[Modifier]("classdiagram.leastMethodModifier") getOrElse {
    SimpleModifier(Accessor.PUBLIC)
  }
  val getterSetterToPublicField: Boolean = prop[Boolean]("classdiagram.getterSetterToPublicField") getOrElse true
  val showConstructorFirst: Boolean = prop[Boolean]("classdiagram.showConstructorFirst") getOrElse true
  val sortField: Boolean = prop[Boolean]("classdiagram.sortField") getOrElse false
  val sortMethod: Boolean = prop[Boolean]("classdiagram.sortMethod") getOrElse false
  val removeDependencyIfExtendOrAggregate: Boolean = prop[Boolean]("classdiagram.removeDependencyIfExtendOrAggregate") getOrElse true
  val removeFieldIfRelateToOther: Boolean = prop[Boolean]("classdiagram.removeFieldIfRelateToOther") getOrElse true
  val showDependencies: Boolean = prop[Boolean]("classdiagram.showDependencies") getOrElse true
  val showExtensions: Boolean = prop[Boolean]("classdiagram.showExtensions") getOrElse true
  val showImplementations: Boolean = prop[Boolean]("classdiagram.showImplementations") getOrElse true
  val showAggregations: Boolean = prop[Boolean]("classdiagram.showAggregations") getOrElse true
  val showCompositions: Boolean = prop[Boolean]("classdiagram.showCompositions") getOrElse true
  val showStaticField: Boolean = prop[Boolean]("classdiagram.showStaticField") getOrElse true
  val showStaticMethod: Boolean = prop[Boolean]("classdiagram.showStaticMethod") getOrElse true
  val showDependencyToSelf: Boolean = prop[Boolean]("classdiagram.showDependencyToSelf") getOrElse false
  val showOtherClassAsBaseClass: Boolean = prop[Boolean]("classdiagram.showOtherClassAsBaseClass") getOrElse true
  val generateFullName: Boolean = prop[Boolean]("classdiagram.generateFullName") getOrElse true
  val showPackage: Boolean = prop[Boolean]("classdiagram.showPackage") getOrElse true
  val centerClass: String = prop[String]("classdiagram.centerClass") getOrElse ""
  val maxDistanceToCenter: Int = prop[Int]("classdiagram.maxDistanceToCenter") getOrElse 1
}