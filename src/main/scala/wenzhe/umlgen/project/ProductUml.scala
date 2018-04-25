package wenzhe.umlgen.project

import wenzhe.umlgen._
import scala.collection.JavaConversions._
import wenzhe.umlgen.config.ConfigurableClassDiagramUml
import java.nio.file.Path
import wenzhe.umlgen.exporter.ClassDiagramExporter
import java.nio.file.Files
import wenzhe.umlgen.factory.FilterableUmlFactory
import wenzhe.umlgen.config.AppConfig

/**
 * @author wen-zhe.liu@asml.com
 *
 */
trait PackageConfig {
  val filterConfig: FilterConfig
  def exporterConfig(name: String): ExporterConfig
  val classDiagramConfig: ClassDiagramConfig
}
trait ProjectConfig extends AppConfig {
  val srcRootDirs: Seq[Path]
  val packageConf: PackageConfig
}
trait ProductConfig extends AppConfig {
  val projects: Seq[ProjectConfig]
}
class ProductDataConfig(
                         val filterConfig: FilterConfig,
                         val exporterConfig: ExporterConfig,
                         val classDiagramConfig: ClassDiagramConfig,
                         val projects: Seq[ProjectConfig]
                       ) extends ProductConfig
class ProjectDataConfig(
                         val filterConfig: FilterConfig,
                         val exporterConfig: ExporterConfig,
                         val classDiagramConfig: ClassDiagramConfig,
                         val srcRootDirs: Seq[Path],
                         val packageConf: PackageConfig
                       ) extends ProjectConfig
abstract class PackageDataConfig(
                         val filterConfig: FilterConfig,
                         val classDiagramConfig: ClassDiagramConfig
                       ) extends PackageConfig {
  def exporterConfig(name: String): ExporterConfig
}
class ClassDiagramDataConfig(
                              val showField: Boolean,
                              val leastFieldModifier: Modifier,
                              val showConstructor: Boolean,
                              val showMethod: Boolean,
                              val onlyShowMethodInInterface: Boolean,
                              val leastMethodModifier: Modifier,
                              val getterSetterToPublicField: Boolean,
                              val showConstructorFirst: Boolean,
                              val sortField: Boolean,
                              val sortMethod: Boolean,
                              val removeDependencyIfExtendOrAggregate: Boolean,
                              val removeFieldIfRelateToOther: Boolean,
                              val showDependencies: Boolean,
                              val showExtensions: Boolean,
                              val showImplementations: Boolean,
                              val showAggregations: Boolean,
                              val showCompositions: Boolean,
                              val showStaticField: Boolean,
                              val showStaticMethod: Boolean,
                              val showDependencyToSelf: Boolean,
                              val showOtherClassAsBaseClass: Boolean,
                              val generateFullName: Boolean,
                              val showPackage: Boolean,
                              val centerClass: String,
                              val maxDistanceToCenter: Int
                            ) extends ClassDiagramConfig
class FilterDataConfig(
val recursiveLevel: Int,
val supportedLanguages: Set[String]
) extends FilterConfig

class ExporterDataConfig (
                           val name: String,
                           val exportToPlantUml: Option[Path],
                           val exportToPNG: Option[Path],
                           val exportToSVG: Option[Path],
                           val printToConsole: Boolean
                         ) extends ExporterConfig


class PackageUml(val uml: ClassDiagramUml, conf: PackageConfig) {
  private lazy val name: String = uml.classes groupBy (_.packageName) maxBy (_._2.size) _1
  lazy val toExporter: UmlExporter = {
    new ClassDiagramExporter(uml, conf exporterConfig name)
  }
}
class ProjectUml(conf: ProjectConfig) {
  private val umlFactory = new FilterableUmlFactory(conf.filterConfig.supportedLanguages, 0)
  private val packages: Seq[PackageUml] = {
    conf.srcRootDirs flatMap (createPackageUmls(_)) map {
      new ConfigurableClassDiagramUml(conf.packageConf.classDiagramConfig, _)
    } map (new PackageUml(_, conf.packageConf))
  }
  private def createPackageUml(files: Traversable[Path]): Option[ClassDiagramUml] = {
    val uml = umlFactory.createClassDiagramUml(files)
    if (uml.isEmpty) None else Some(uml)
  }
  private def createPackageUmls(prjSrcRootDir: Path): Traversable[ClassDiagramUml] = if (Files isDirectory prjSrcRootDir) {
    val (subDirs, files) = Files.list(prjSrcRootDir).iterator.toStream.partition(Files isDirectory _)
    createPackageUml(files) ++ subDirs.flatMap(createPackageUmls(_))
  } else Nil
  lazy val uml: ClassDiagramUml = {
    new ConfigurableClassDiagramUml(conf.classDiagramConfig, new ComposableClassDiagram(packages.map(_.uml)))
  }
  lazy val toExporter: UmlExporter = {
    new GroupUmlExporter(new ClassDiagramExporter(uml, conf.exporterConfig) +: packages.map(_.toExporter))
  }
}
class ProductUml(conf: ProductConfig) {
  private val projects: Seq[ProjectUml] = conf.projects.map(new ProjectUml(_))
  lazy val uml: ClassDiagramUml = {
    new ConfigurableClassDiagramUml(conf.classDiagramConfig, new ComposableClassDiagram(projects.map(_.uml)))
  }
  lazy val toExporter: UmlExporter = {
    new GroupUmlExporter(new ClassDiagramExporter(uml, conf.exporterConfig) +: projects.map(_.toExporter))
  }
}
