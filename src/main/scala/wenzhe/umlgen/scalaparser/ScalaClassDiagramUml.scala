package wenzhe.umlgen.scalaparser

import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.reflect.internal.util.BatchSourceFile
import scala.io.Source
import wenzhe.umlgen._
import java.nio.file.Path
import ClassAttr._

object scalaparser {
  val settings = new Settings
  val global = Global(settings, new ConsoleReporter(settings))
  import global.Run
  val run = new Run
}

import scalaparser._
import scalaparser.global
import scalaparser.global.{Type => _, _}
import global.ImplDef
import global.PackageDef

/**
 * @author wen-zhe.liu@asml.com
 *
 */

case class ScalaUmlClass(cls: ImplDef, pkg: PackageDef)

object ScalaUmlFactory extends UmlFactory {
  def createClassDiagramUml(files: Traversable[Path]) = new ScalaClassDiagramUml ( 
    files.map(_.toString).map { path =>
      val fileContent = Source.fromFile(path).mkString
      new BatchSourceFile(path, fileContent)
    }.map(new syntaxAnalyzer.SourceFileParser(_)).map(_.parse).collect {
      case pkg @ PackageDef(packaged, children) => 
        children flatMap getTypeRecursively map (ScalaUmlClass(_, pkg))
    }.flatten
  )
  private def getTypeRecursively(tree: Tree): Traversable[ImplDef] = tree match {
    case cls: ImplDef =>
      cls :: (cls.children flatMap getTypeRecursively)
    case Template(_, _, body) => body flatMap getTypeRecursively
    case _ => Nil
  }
}

class ScalaClassDiagramUml(umlClses: Traversable[ScalaUmlClass]) extends ClassDiagramUml {
  lazy val umlClasses: Traversable[ClassDesc] = {
    umlClses map (new ScalaClassDesc(_))
  }
  lazy val relations: Traversable[Relation] = {
    umlClasses flatMap (new ClassRelationFactory(this, _) createRelations)
  }

  val skipClasses: Set[String] = Set("scala.AnyRef", "scala.Product", "scala.Serializable", "Enumeration")

  lazy val extraClasses: Traversable[ClassDesc] = 
    umlClasses flatMap (_.parents) map (_.firstName) filterNot isSkipClass filterNot isUmlClass map {
      ExtraClass(isInterfaceOrNot = true, _)
    }
  
  private lazy val enums: Set[String] = umlClses map (_.cls) filter isEnumType map (_.name.toString) toSet
  private def isEnumType(cls: ImplDef): Boolean = {
    cls.impl.parents map (_.toString) exists (_ == "Enumeration")
  }
  private def isEnumType(name: String): Boolean = enums contains name
  
  class ScalaClass(cls: ImplDef) extends ClassDesc {
    lazy val classAttr: ClassAttr = {
      val mods = cls.mods
      if (mods.isTrait || mods.isInterface) INTERFACE
      else if (mods hasAbstractFlag) ABSTRACT_CLASS
      else if (cls.impl.parents map (_.toString) exists (_ == "Enumeration")) ENUM
      else CLASS
    }
    lazy val enumConstants: Traversable[String] = {
      if (isEnum) cls.impl.body collect {
        case ValDef(_, name, _, _) => name.toString
      } else Nil
    }
    lazy val name: String = cls match {
      case ModuleDef(_, moduleName, _) if !isEnum => moduleName.toString + "$" // object
      case c => c.name.toString  // class
    }
    lazy val fields: Traversable[ScalaField] = cls.impl.body collect {
      case v: ValDef if !isEnum => new ScalaField(v)
    }
    lazy val methods: Traversable[ScalaMethod] = cls.impl.body collect {
        case m: DefDef => new ScalaMethod(m)
      } filterNot { method => 
        List("$init$", "<init>").contains(method.name) && method.parameters.isEmpty
      }
    lazy val packageName: String = "" // need trait ScalaPackage
    lazy val fullName: String = "" //TODO: consider nested class
    val language: String = "scala"
    override lazy val parents: List[ScalaType] = cls.impl.parents map (new ScalaType(_))
    lazy val extendTypes: List[ScalaType] = {
      if (parents.isEmpty) Nil
      else if (ScalaClassDiagramUml.this isInterface parents.head) Nil
      else if (isInterface) parents
      else List(parents.head)
    }
    lazy val implementedTypes: List[ScalaType] = parents diff extendTypes
  }
  
  //TODO: with trait ScalaPackage
  class ScalaClassDesc(umlClass: ScalaUmlClass) extends ScalaClass(umlClass.cls) {
    override lazy val packageName: String = umlClass.pkg.pid.toString
  }
  class ScalaField(v: ValDef) extends Field {
    lazy val modifier: ScalaModifier = new ScalaModifier(v.mods)
    lazy val name: String = v.name.toString
    lazy val fieldType: ScalaTypeWithAutoDefer = new ScalaTypeWithAutoDefer(v)
  }
  class ScalaType(t: Tree) extends Type {
    protected object Constant {
      val COLLECTIONS = Set("List", "Array", "Vector", "Set", "HashSet", "TreeSet", "Seq",
          "Traversable", "Iterable", "ArrayBuffer")
      val MAPS = Set("Map", "HashMap", "TreeMap", "EnumMap")
      val OPTIONS = Set("Option")
    }
    import Constant._
    lazy val name: String = t match {
      case _: ExistentialTypeTree => t.toString takeWhile(_ != '[')
      case _ => 
        val str = t.toString
        if (str == "<type ?>") "" 
        else if (str endsWith ".Value") {
          val prefix = str dropRight ".Value".length
          if (isEnumType(prefix)) prefix
          else str
        }
        else str
    }
    lazy val isUmlClass: Boolean = ScalaClassDiagramUml.this isUmlClass this
    lazy val simpleName: String = t match {
      case AppliedTypeTree(tpt, args) => 
        val clsName = tpt.toString
        if (args.size == 1 && (COLLECTIONS ++ OPTIONS contains clsName)) new ScalaType(args(0)).simpleName 
        else if (args.size == 2 && (MAPS contains clsName)) new ScalaType(args(1)).simpleName
        else clsName
      case _: ExistentialTypeTree => t.toString takeWhile(_ != '[')
      case Apply(fun, _) => fun.toString  // handle "("
      case s => s.toString
    }
    lazy val firstName: String = t match {
      case AppliedTypeTree(tpt, args) => tpt.toString
      case _: ExistentialTypeTree => t.toString takeWhile(_ != '[')
      case Apply(fun, _) => fun.toString  // handle "("
      case s => s.toString
    }
    lazy val isCollection: Boolean = COLLECTIONS ++ MAPS contains firstName
    lazy val isOption: Boolean = OPTIONS contains firstName
  }
  class ScalaTypeWithAutoDefer(v: ValOrDefDef) extends ScalaType(v.tpt) { //TODO:with trait AutoDefer
    override lazy val name: String = {
      val str = super.name
      if (str.isEmpty) {
        val NewStatement = """new\s+(\w+).*""".r
        val NewCaseObjectStatement = """([A-Z]\w*)\s*\(.+""".r
        val IntValue = """-?\d+""".r
        val DoubleValue = """-?\d*\.\d+d?""".r
        val StringValue = """ ".*" """.trim.r
        v.rhs.toString.trim match {
          case NewStatement(className) => className
          case NewCaseObjectStatement(className) => className
          case IntValue() => "Int"
          case DoubleValue() => "Double"
          case StringValue() => "String"
          case "true" | "false" => "Boolean"
          case _ => ""
        }
      } else str
    }
  }
  
  class ScalaMethod(m: DefDef) extends Method {
    lazy val modifier: Modifier = new ScalaModifier(m.mods)
    lazy val name: String = m.name.toString
    lazy val parameters: List[ScalaParameter] = for {
        vps <- m.vparamss
        param <- vps
      } yield new ScalaParameter(param)
    lazy val returnType: Option[ScalaTypeWithAutoDefer] = {
      Some(new ScalaTypeWithAutoDefer(m)) filterNot (_.name isEmpty)
    }
    override lazy val isConstructor: Boolean = name == "<init>"
  }
  
  class ScalaParameter(v: ValDef) extends Parameter {
    lazy val name: String = v.name.toString
    lazy val paramType: ScalaType = new ScalaType(v.tpt)
  }
  
  class ScalaModifier(mods: Modifiers) extends Modifier {
    import Accessor._
    lazy val accessor: Accessor = {
      if (mods isPublic) PUBLIC
      else if (mods isProtected) PROTECTED
      else if (mods hasPackageFlag) PACKAGE
      else if (mods isPrivate) PRIVATE
      else PUBLIC
    }
    lazy val isStatic: Boolean = mods hasStaticFlag
    lazy val isAbstract: Boolean = mods hasAbstractFlag
    lazy val isFinal: Boolean = !mods.isMutable || mods.isFinal
  }
}
