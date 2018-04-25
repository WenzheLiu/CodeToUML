package wenzhe.umlgen.console

import java.nio.file.{Path, Paths}

import wenzhe.umlgen.ImplicitHelper._
import wenzhe.umlgen.config.{ConfigurableClassDiagramUml, ProductMapConfig, PropParser, UmlPropertyConfig}
import wenzhe.umlgen.exporter.ClassDiagramExporter
import wenzhe.umlgen.factory.FilterableUmlFactory
import wenzhe.umlgen.project.ProductUml
import wenzhe.umlgen.ui.fx.MainFrameRun

/**
 * @author wen-zhe.liu@asml.com
 *
 */
object Main extends App {
  if (args isEmpty) {
    //System.err println "Argument should not be empty"
    MainFrameRun.main(args)
    System exit 0
  }
  PropParser.prop[String]("uml.project").map { prjPath =>
    Paths get prjPath toAbsolutePath
  }.map(parseJson _).flatten.map { mp =>
    if (args.size != 2) {
      System.err println "Argument should be 2 for project mode"
      System exit -1
    }
    println("Running in Project Mode")
    val rootPathOfProduct: Path = Paths.get(args(0)).toAbsolutePath
    val rootPathToExport: Path = Paths.get(args(1)).toAbsolutePath
    new ProductMapConfig(mp, rootPathOfProduct, rootPathToExport)
  }.map(new ProductUml(_).toExporter).getOrElse {
    println("Running in File/Folder Mode")
    val conf = new UmlPropertyConfig
    val files = args.toStream map (Paths get _ toAbsolutePath)
    val umlFac = new FilterableUmlFactory(conf filterConfig)
    val uml = new ConfigurableClassDiagramUml(conf classDiagramConfig, umlFac createClassDiagramUml files)
    new ClassDiagramExporter(uml, conf exporterConfig)
  }.export()
  println("Done!")
}

/* 
图片精度 -> svg
FxScala -> eclipse
画UML chart的代码，控制图片大小，自定义GUI
package : show full name in class and relation
行为图
enum
jar parser
抽象类
Eclipse扩展：基于plantuml开源项目，写自己的插件
组合与聚合
dependency by code body （虚线依赖，方法参数的依赖为实线）
配置：以某个类为中心，只显示于之相连的类（1层），或间接相连（n层）
APP参数，递归查找还是不递归
重构：重用ClassDesc代码，去重复逻辑
协变逆变：方法参数类型抽象层次尽可能高，返回值类型层次尽可能低，这样不仅无害，还可提高处理能力，减少强制类型转换
support Tuple type to field/method
设计用OOP，实现用FP，无副作用纯函数，不可变类，不可变对象。
bug: field type is : "Relation with RelationNote" cannot create link
bug: scala impl cannot show full name
support code body dependency
project mode config, sub config cannot overwrite parent config
修改UML图外观设置
Fix SBT build warning issues
 */

