package wenzhe.umlgen.javaparser

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import scala.collection.JavaConversions._
import scala.compat.java8.OptionConverters._
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.{Modifier => JpModifier}
import com.github.javaparser.ast.body.{Parameter => JpParameter}
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.`type`.{ClassOrInterfaceType, Type => JpType}
import java.nio.file.Path
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.`type`.ArrayType
import java.util.concurrent.atomic.AtomicReference
import java.util.EnumSet
import com.github.javaparser.ast.{Modifier => JpModifier}
import com.github.javaparser.ast.`type`.{Type => JpType}
import com.github.javaparser.ast.body.{Parameter => JpParameter}
import wenzhe.umlgen._
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.EnumDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithExtends
import com.github.javaparser.ast.nodeTypes.NodeWithImplements
import com.github.javaparser.ast.CompilationUnit
import ClassAttr._
import Accessor._

/**
 * @author wen-zhe.liu@asml.com
 *
 */
object JavaUmlFactory extends UmlFactory {
  def createClassDiagramUml(files: Traversable[Path]) = new JavaClassDiagramUml(
    files map (JavaParser parse _) flatMap { compileUnit =>
      getTypesRecursively(compileUnit) map (JavaUmlClass(compileUnit, _))
    }
  )
  
  private def getTypesRecursively(node: Node): Traversable[TypeDeclaration[_]] = node match {
    case c: CompilationUnit => c.getTypes flatMap getTypesRecursively
    case t: TypeDeclaration[_] => t :: (node.getChildNodes.toList flatMap getTypesRecursively)
    case _ => Nil
  }
}

private case class JavaUmlClass(compileUnit: CompilationUnit, classDeclaration: TypeDeclaration[_])

class JavaClassDiagramUml(umlClses: Traversable[JavaUmlClass]) extends ClassDiagramUml {
  private val classDeclarations = umlClses map (_.classDeclaration)
  lazy val umlClasses: Traversable[ClassDesc] = {
    umlClses map (new JavaClassDesc(_))
  }
  
  lazy val relations: Traversable[Relation] = {
    classes flatMap (new ClassRelationFactory(this, _) createRelations)
  }
  
  val skipClasses: Set[String] = Set("Object")

  lazy val extraClasses: Traversable[ClassDesc] = {
    val extendedClassesBesidesUmlClass = classDeclarations.collect {
      case c: ClassOrInterfaceDeclaration =>
        c.getExtendedTypes map (_.getNameAsString) filterNot isSkipClass filterNot isUmlClass map {
          ExtraClass(c.isInterface, _)
        }
    }.flatten.toSet
    val implementedClassesBesidesUmlClass: Traversable[ClassDesc] = classDeclarations.collect {
      case c: NodeWithImplements[_] =>
        c.getImplementedTypes map (_.getNameAsString) filterNot isUmlClass map {
          ExtraClass(isInterfaceOrNot = true, _)
        }
    }.flatten.toSet
    extendedClassesBesidesUmlClass ++ implementedClassesBesidesUmlClass
  }
  
  class JavaType private(t: JpType) extends Type {
    private object Constant {
      val COLLECTIONS = Set("List", "ArrayList", "Vector", "Set", "HashSet", "TreeSet", "EnumSet", 
          "Collection", "Iterable")
      val MAPS = Set("Map", "HashMap", "TreeMap", "EnumMap", "BiMap")
      val OPTIONS = Set("Optional")
    }
    import Constant._
    val name: String = t asString
    lazy val simpleName: String = t match {
      case c: ClassOrInterfaceType => {
        val clsName = c.getNameAsString
        val typeArgs = c.getTypeArguments.asScala
        if (typeArgs.size == 1 && (COLLECTIONS ++ OPTIONS contains clsName)) JavaType(typeArgs.get.get(0)).simpleName
        else if (typeArgs.size == 2 && (MAPS contains clsName)) JavaType(typeArgs.get.get(1)).simpleName
        else clsName
      }
      case a: ArrayType => JavaType(a.getComponentType).simpleName
      case tp => tp.asString()
    }
    lazy val isUmlClass: Boolean = JavaClassDiagramUml.this isUmlClass this
    lazy val firstName: String = t match {
      case c: ClassOrInterfaceType => c.getNameAsString
      case tp => tp.asString
    }
    lazy val isCollection: Boolean = t match {
      case _: ArrayType => true
      case _ => (COLLECTIONS ++ MAPS) contains firstName
    }
    lazy val isOption: Boolean = OPTIONS contains firstName
    lazy val genericArgTypes = JavaType extractTypeArgsFrom t map (JavaType(_))
  }
  private object JavaType {
    val cache = new AtomicReference[Map[String, JavaType]](Map())
    def apply(t: JpType): JavaType = {
      val name = t.asString()
      cache.get.getOrElse(name, {
        val jt = new JavaType(t)
        cache.set(cache.get + (name -> jt))
        jt
      })
    }
    private def extractTypeArgsFrom(t: JpType): Seq[JpType] = t match {
      case c: ClassOrInterfaceType if !c.isBoxedType => 
        c.getTypeArguments.asScala.map(_ flatMap { typeArg => 
          typeArg +: extractTypeArgsFrom(typeArg)
        }).getOrElse(Nil)
      case a: ArrayType => 
        val compType = a.getComponentType
        compType +: extractTypeArgsFrom(compType)
      case _ => Nil
    }
  }
  
  class JavaClassDesc(umlClass: JavaUmlClass) extends ClassDesc {
    private lazy val cls: TypeDeclaration[_] = umlClass classDeclaration
    lazy val classAttr: ClassAttr = cls match {
      case c: ClassOrInterfaceDeclaration => 
        if (c isInterface) INTERFACE
        else if (cls.getModifiers contains JpModifier.ABSTRACT) ABSTRACT_CLASS
        else CLASS
      case _: EnumDeclaration => ENUM
      case _ => CLASS
    }
    val name: String = cls getNameAsString
    lazy val enumConstants: Traversable[String] = cls match {
      case e: EnumDeclaration => e.getEntries map (_.getNameAsString)
      case _ => Nil
    }
    lazy val fields: Traversable[Field] = for {
      field <- cls getFields()
      variable <- field getVariables()
    } yield new JavaField(field, variable)
  
    lazy val methods: Traversable[Method] = cls.getMembers collect {
      case c: ConstructorDeclaration => new JavaConstructor(c)
      case m: MethodDeclaration => new JavaMethod(m)
    }
    lazy val extendTypes: List[JavaType] = (cls match {
      case c: NodeWithExtends[_] => c.getExtendedTypes.toList
      case _ => Nil
    }).map(JavaType(_))
    lazy val implementedTypes: List[JavaType] = (cls match {
      case c: NodeWithImplements[_] => c.getImplementedTypes.toList
      case _ => Nil
    }).map(JavaType(_))

    lazy val packageName: String = {
      umlClass.compileUnit.getPackageDeclaration.asScala map (_.getNameAsString) getOrElse ""
    }
    
    lazy val fullName: String = {
      def nestedTypes(node: Node): List[String] = node match {
        case t: TypeDeclaration[_] => 
          t.getNameAsString :: (node.getParentNode.asScala map nestedTypes getOrElse Nil)
        case _ => Nil
      }
      packageName + "." + (nestedTypes(cls).reverse mkString "$")
    }
    
    val language: String = "java"
    
    class JavaField(field: FieldDeclaration, variable: VariableDeclarator) extends Field {
      val modifier: Modifier = new JavaModifier(field.getModifiers)
      val name: String = variable getNameAsString
      val fieldType: Type = JavaType(variable.getType)
    }
    
    abstract class JavaCallable[T<:Node](callable: CallableDeclaration[T]) extends Method {
      val modifier: Modifier = new JavaModifier(callable getModifiers)
      val name: String = callable.getName.getIdentifier
      lazy val parameters: Traversable[Parameter] = {
        callable.getParameters map (new JavaParameter(_))
      }
    }
    
    class JavaConstructor(ctor: ConstructorDeclaration) extends JavaCallable(ctor) {
      val returnType: Option[Type] = None
    }
    
    class JavaMethod(method: MethodDeclaration) extends JavaCallable(method) with MayBeBean {
      val returnType: Option[Type] = Some(JavaType(method.getType))
      private def isCommonToGetterSetter(prefix: String): Boolean = {
        name.startsWith(prefix) && name.length > prefix.length && name(prefix.length).isUpper && modifier.isPublic
      }
      lazy val isGetter: Boolean = {
        isCommonToGetterSetter("get") && parameters.isEmpty && returnType.get.name != "void"
      }
      lazy val isSetter: Boolean = {
        isCommonToGetterSetter("set") && parameters.size == 1
      }
      lazy val bean: Option[String] = {
        if (isGetter || isSetter) Some {
          val start = "get".length
          name(start).toLower +: (if (name.length == start + 1) "" else name.substring(start + 1))
        } else None
      }
    }
    
    class JavaModifier(modifiers: EnumSet[JpModifier]) extends Modifier {
      lazy val accessor: Accessor = {
        if (modifiers contains JpModifier.PUBLIC) PUBLIC
        else if (modifiers contains JpModifier.PROTECTED) PROTECTED
        else if (modifiers contains JpModifier.DEFAULT) PACKAGE
        else if (modifiers contains JpModifier.PRIVATE) PRIVATE
        else if (isInterface) PUBLIC
        else PACKAGE
      }
      lazy val isStatic: Boolean = modifiers contains JpModifier.STATIC
      lazy val isAbstract: Boolean = modifiers contains JpModifier.ABSTRACT
      lazy val isFinal: Boolean = modifiers contains JpModifier.FINAL
    }
    
    class JavaParameter(param: JpParameter) extends Parameter {
      val name: String = param getNameAsString
      val paramType: Type = JavaType(param.getType)
    }
  }
}
