package wenzhe.umlgen.config

import wenzhe.umlgen.project.ProductConfig
import wenzhe.umlgen._
import java.nio.file.Path
import wenzhe.umlgen.ImplicitHelper._
import wenzhe.umlgen.project.ProjectConfig
import wenzhe.umlgen.project.PackageConfig

/**
 * @author wen-zhe.liu@asml.com
 *
 */
abstract class MapConfig(mp: Map[String, Any]) {
  protected def get(key: String, default: => Boolean): Boolean = mp get key match {
    case Some(v: Boolean) => v
    case _ => default
  }
  protected def get(key: String, default: => String): String = mp get key match {
    case Some(v: String) => v
    case _ => default
  }
  protected def getString(key: String): Option[String] = mp get key collect {
    case Some(v: String) => v
  }
  protected def get(key: String, default: => Int): Int = mp get key match {
    case Some(v: Int) => v
    case _ => default
  }
  protected def getMap(key: String): Map[String, Any] = (mp get key match {
    case Some(m: Map[_, _]) => m
    case _ => Map.empty
  }) collect {
    case (k: String, v: Any) => (k, v)
  }
  protected def getSeq(key: String): Seq[_] = mp get key match {
    case Some(v: Seq[_]) => v
    case _ => Nil
  }
  protected def getMaps(key: String): Seq[Map[String, Any]] = getSeq(key) collect {
    case m: Map[_, _] => m collect {
      case (k: String, v: Any) => (k, v)
    }
  }
  protected def getStrings(key: String): Seq[String] = getSeq(key) collect {
    case v: String => v
  }
}

class PackageMapConfig(mp: Map[String, Any], parentConf: InnerConfig, projectExportRoot: Path) 
    extends MapConfig(mp) with PackageConfig {
  private val config: InnerConfig = new InnerMapConfig(getMap("config"), parentConf)
  val filterConfig: FilterConfig = new ProductFilterConfig(config)
  def exporterConfig(name: String): ExporterConfig = {
    new ProductExporterConfig(name, projectExportRoot, config)
  }
  val classDiagramConfig: ClassDiagramConfig = config.classDiagram
}

class ProjectMapConfig(mp: Map[String, Any], parentConf: InnerConfig, rootPathOfProduct: Path, productExportRoot: Path) 
    extends MapConfig(mp) with ProjectConfig {
  private val prjRootPath: Path = rootPathOfProduct / get("path", ".")
  private val name: String = get("name", prjRootPath.getFileName.toString)
  private val config: InnerConfig = new InnerMapConfig(getMap("config"), parentConf)
  private val prjExportRoot: Path = productExportRoot / name
  
  val srcRootDirs: Seq[Path] = getStrings("sources") map (prjRootPath resolve _)
  val packageConf: PackageConfig = new PackageMapConfig(getMap("packageConfig"), config, prjExportRoot)
  
  val filterConfig: FilterConfig = new ProductFilterConfig(config)
  val exporterConfig: ExporterConfig = new ProductExporterConfig(name, prjExportRoot, config)
  val classDiagramConfig: ClassDiagramConfig = config.classDiagram
}

class ProductMapConfig(mp: Map[String, Any], rootPathOfProduct: Path, rootPathToExport: Path) 
    extends MapConfig(mp) with ProductConfig {
  private val name: String = get("name", rootPathOfProduct.getFileName.toString)
  private val config: InnerConfig = new InnerMapConfig(getMap("config"), DefaultInnerConfig)
  val projects: Seq[ProjectConfig] = getMaps("projects").map {
    new ProjectMapConfig(_, config, rootPathOfProduct, rootPathToExport)
  }
  
  val filterConfig: FilterConfig = new ProductFilterConfig(config)
  val exporterConfig: ExporterConfig = new ProductExporterConfig(name, rootPathToExport, config)
  val classDiagramConfig: ClassDiagramConfig = config.classDiagram
}

class ProductFilterConfig(config: InnerConfig) extends FilterConfig {
  val recursiveLevel: Int = Int.MaxValue
  lazy val supportedLanguages: Set[String] = config.supportedLanguages
}
class ProductExporterConfig(val name: String, rootPathOfExport: Path, config: InnerConfig) extends ExporterConfig {
  lazy val exportToPlantUml: Option[Path] = if (!config.exportToPlantUml) None else Some {
    rootPathOfExport / (name + ".uml")
  }
  lazy val exportToPNG: Option[Path] = if (!config.exportToPNG) None else Some {
    rootPathOfExport / (name + ".png")
  }
  lazy val exportToSVG: Option[Path] = if (!config.exportToSVG) None else Some {
    rootPathOfExport / (name + ".svg")
  }
  val printToConsole: Boolean = false
}

trait InnerConfig {
  val supportedLanguages: Set[String]
  val exportToPlantUml: Boolean
  val exportToPNG: Boolean
  val exportToSVG: Boolean
  val classDiagram: ClassDiagramConfig
}

class InnerMapConfig(mp: Map[String, Any], default: InnerConfig)
    extends MapConfig(mp) with InnerConfig {
  val supportedLanguages: Set[String] = mp get "supportedLanguages" match {
    case Some(v: String) => v split ";" toSet
    case _ => default.supportedLanguages
  }
  val exportToPlantUml: Boolean = get("exportToPlantUml", default.exportToPlantUml)
  val exportToPNG: Boolean = get("exportToPNG", default.exportToPNG)
  val exportToSVG: Boolean = get("exportToSVG", default.exportToSVG)
  val classDiagram: ClassDiagramConfig = new ClassDiagramMapConfig(getMap("classDiagram"), default.classDiagram)
}

object DefaultInnerConfig extends InnerConfig {
  val supportedLanguages: Set[String] = Set("java", "scala")
  val exportToPlantUml: Boolean = false
  val exportToPNG: Boolean = false
  val exportToSVG: Boolean = false
  val classDiagram: ClassDiagramConfig = DefaultClassDiagramConfig
}

class ClassDiagramMapConfig(mp: Map[String, Any], default: ClassDiagramConfig) 
    extends MapConfig(mp) with ClassDiagramConfig{
  val showField: Boolean = get("showField", default.showField)
  val leastFieldModifier: Modifier = {
    getString("leastFieldModifier") map (Modifier createByAccessorName _) getOrElse default.leastFieldModifier
  }
  val showConstructor: Boolean = get("showConstructor", default.showConstructor)
  val showMethod: Boolean = get("showMethod", default.showMethod)
  val onlyShowMethodInInterface: Boolean = get("onlyShowMethodInInterface", default.onlyShowMethodInInterface)
  val leastMethodModifier: Modifier = {
    getString("leastMethodModifier") map (Modifier createByAccessorName _) getOrElse default.leastMethodModifier
  }
  val getterSetterToPublicField: Boolean = get("getterSetterToPublicField", default.getterSetterToPublicField)
  val showConstructorFirst: Boolean = get("showConstructorFirst", default.showConstructorFirst)
  val sortField: Boolean = get("sortField", default.sortField)
  val sortMethod: Boolean = get("sortMethod", default.sortMethod)
  val removeDependencyIfExtendOrAggregate: Boolean = {
    get("removeDependencyIfExtendOrAggregate", default.removeDependencyIfExtendOrAggregate)
  }
  val removeFieldIfRelateToOther: Boolean = get("removeFieldIfRelateToOther", default.removeFieldIfRelateToOther)
  val showDependencies: Boolean = get("showDependencies", default.showDependencies)
  val showDependencyToSelf: Boolean = get("showDependencyToSelf", default.showDependencyToSelf)
  val showExtensions: Boolean = get("showExtensions", default.showExtensions)
  val showImplementations: Boolean = get("showImplementations", default.showImplementations)
  val showAggregations: Boolean = get("showAggregations", default.showAggregations)
  val showCompositions: Boolean = get("showCompositions", default.showCompositions)
  val showStaticField: Boolean = get("showStaticField", default.showStaticField)
  val showStaticMethod: Boolean = get("showStaticMethod", default.showStaticMethod)
  val showOtherClassAsBaseClass: Boolean = get("showOtherClassAsBaseClass", default.showOtherClassAsBaseClass)
  val generateFullName: Boolean = get("generateFullName", default.generateFullName)
  val showPackage: Boolean = get("showPackage", default.showPackage)
  val centerClass: String = get("centerClass", default.centerClass)
  val maxDistanceToCenter: Int = get("maxDistanceToCenter", default.maxDistanceToCenter)
}

