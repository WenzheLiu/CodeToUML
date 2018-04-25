package wenzhe.umlgen

import java.nio.file.Path

/**
 * @author wen-zhe.liu@asml.com
 *
 */
trait UmlGenerator {
  def generate: String
}

trait UmlFactory {
  def createClassDiagramUml(files: Traversable[Path]): ClassDiagramUml
}

object RelationType extends Enumeration {
  type RelationType = Value
  val DEPEND, ASSOCIATION, AGGREGATION, COMPOSITION, EXTEND_BY, IMPLEMENT_BY = Value
}
import wenzhe.umlgen.RelationType._

object ClassAttr extends Enumeration {
  type ClassAttr = Value
  val INTERFACE, ABSTRACT_CLASS, CLASS, ENUM = Value
}
import wenzhe.umlgen.ClassAttr._

object Accessor extends Enumeration {
  type Accessor = Value
  val PRIVATE, PACKAGE, PROTECTED, PUBLIC = Value
}
import wenzhe.umlgen.Accessor._

trait ClassDiagramUml {
  val umlClasses: Traversable[ClassDesc]
  val extraClasses: Traversable[ClassDesc]
  val skipClasses: Set[String]
  val relations: Traversable[Relation]

  def classes: Traversable[ClassDesc] = umlClasses ++ extraClasses
  def isSkipClass(className: String): Boolean = skipClasses contains className
  def isSkipClass(cls: Type): Boolean = isSkipClass(cls.simpleName)
  def findClassByName(name: String): Option[ClassDesc] = classes find (_.name == name)
  def findUmlClassByName(name: String): Option[ClassDesc] = umlClasses find (_.name == name)
  def findExtraClassByName(name: String): Option[ClassDesc] = extraClasses find (_.name == name)
  def isUmlClass(className: String): Boolean = findUmlClassByName(className) isDefined
  def isUmlClass(cls: Type): Boolean = isUmlClass(cls.simpleName)
  def isInterface(className: String): Boolean = {
    findUmlClassByName(className) map (_.isInterface) getOrElse true
  }
  def isInterface(cls: Type): Boolean = {
    isInterface(cls.simpleName)
  }
  def findUmlClassByType(tpt: Type): Option[ClassDesc] = {
    findUmlClassByName(tpt.simpleName)
  }
  def isEmpty: Boolean = classes.isEmpty
}

trait ClassDiagramConfig {
  val showField: Boolean
  val leastFieldModifier: Modifier
  val showConstructor: Boolean
  val showMethod: Boolean
  val onlyShowMethodInInterface: Boolean
  val leastMethodModifier: Modifier
  val getterSetterToPublicField: Boolean
  val showConstructorFirst: Boolean
  val sortField: Boolean
  val sortMethod: Boolean
  val removeDependencyIfExtendOrAggregate: Boolean
  val removeFieldIfRelateToOther: Boolean
  val showDependencies: Boolean
  val showExtensions: Boolean
  val showImplementations: Boolean
  val showAggregations: Boolean
  val showCompositions: Boolean
  val showStaticField: Boolean
  val showStaticMethod: Boolean
  val showDependencyToSelf: Boolean
  val showOtherClassAsBaseClass: Boolean
  val generateFullName: Boolean
  val showPackage: Boolean
  val centerClass: String
  val maxDistanceToCenter: Int
}
object DefaultClassDiagramConfig extends ClassDiagramConfig {
  val showField: Boolean = true
  val leastFieldModifier: Modifier = SimpleModifier(Accessor.PRIVATE)
  val showConstructor: Boolean = true
  val showMethod: Boolean = true
  val onlyShowMethodInInterface: Boolean = false
  val leastMethodModifier: Modifier = SimpleModifier(Accessor.PROTECTED)
  val getterSetterToPublicField: Boolean = true
  val showConstructorFirst: Boolean = true
  val sortField: Boolean = false
  val sortMethod: Boolean = false
  val removeDependencyIfExtendOrAggregate: Boolean = true
  val removeFieldIfRelateToOther: Boolean = true
  val showDependencies: Boolean = true
  val showDependencyToSelf: Boolean = false
  val showExtensions: Boolean = true
  val showImplementations: Boolean = true
  val showAggregations: Boolean = true
  val showCompositions: Boolean = true
  val showStaticField: Boolean = true
  val showStaticMethod: Boolean = true
  val showOtherClassAsBaseClass: Boolean = true
  val generateFullName: Boolean = true
  val showPackage: Boolean = false
  val centerClass: String = ""
  val maxDistanceToCenter: Int = 1
}
class BeanProvider(classDesc: ClassDesc) {
  protected def beansIf(test: MayBeGetterSetter => Boolean): Traversable[String] = {
    classDesc.methods.collect {
      case method: MayBeBean if test(method) => method.bean
    }.flatten match {
      case beans: Seq[String] => beans.distinct
      case beans => beans
    }
  }
  lazy val getterBeans: Traversable[String] = beansIf (_.isGetter)
  lazy val setterBeans: Traversable[String] = beansIf (_.isSetter)
  lazy val beans: Traversable[String] = {
    val getters = getterBeans.toSet
    val setters = setterBeans.toSet
    getters intersect setters
  }
}



trait ClassDesc {
  val classAttr: ClassAttr
  val enumConstants: Traversable[String]
  val name: String
  val fields: Traversable[Field]
  val methods: Traversable[Method]
  val packageName: String
  val fullName: String
  val language: String
  val extendTypes: Traversable[Type]
  val implementedTypes: Traversable[Type]
  
  final def isInterface: Boolean = classAttr == INTERFACE
  final def isAbstractClass: Boolean = classAttr == ABSTRACT_CLASS
  final def isEnum: Boolean = classAttr == ENUM
  lazy val tip: Option[String] = {
    Option(fullName) filterNot (_ isEmpty) map (language + ":" + _)
  }
  lazy val parents: Traversable[Type] = extendTypes ++ implementedTypes
  lazy val constructors: Traversable[Method] = methods filter (_.isConstructor)
}

trait Parameter {
  val name: String
  val paramType: Type
}

trait Type {
  val name: String
  val simpleName: String
  val firstName: String
  val isUmlClass: Boolean
  val isCollection: Boolean
  val isOption: Boolean
}

trait Field {
  val modifier: Modifier
  val name: String
  val fieldType: Type
  
  lazy val toPublic: Field = {
    val outer = this
    if (modifier.isPublic) this
    else new Field {
      lazy val modifier: Modifier = outer.modifier toPublic
      lazy val name: String = outer.name
      lazy val fieldType: Type = outer.fieldType
    }
  }
}

trait Method {
  val modifier: Modifier
  val name: String
  val parameters: Traversable[Parameter]
  val returnType: Option[Type]
  
  def isConstructor: Boolean = returnType isEmpty
}

trait MayBeGetterSetter {
  val isGetter: Boolean
  val isSetter: Boolean
}

trait MayBeBean extends MayBeGetterSetter {
  val bean: Option[String]
}

trait Relation {
  val leftClass: ClassDesc
  val relationType: RelationType
  val rightClass: ClassDesc
  
  def isSameDirectionWith(other: Relation): Boolean = {
    leftClass.name == other.leftClass.name && rightClass.name == other.rightClass.name
  }
  def isReverseDirectionWith(other: Relation): Boolean = {
    leftClass.name == other.rightClass.name && rightClass.name == other.leftClass.name
  }
  def isSelfRelation: Boolean = leftClass.name == rightClass.name
}

trait RelationNote {
  val leftNote: String
  val rightNote: String
  val name: String
}

case class ClassRelation(leftClass: ClassDesc, relationType: RelationType, rightClass: ClassDesc) extends Relation

case class ClassAggregate(leftClass: ClassDesc, rightClass: ClassDesc, field: Field) 
    extends Relation with RelationNote {
  lazy val relationType: RelationType.Value = {
    def hasFieldTypeInMethodParamTypes(methods: Traversable[Method]): Boolean = {
      methods flatMap (_.parameters) map (_.paramType.simpleName) exists (_ == field.fieldType.simpleName)
    }
    if (field.modifier.isFinal && !hasFieldTypeInMethodParamTypes(leftClass.constructors)) COMPOSITION
    else if (!field.modifier.isPublic && !hasFieldTypeInMethodParamTypes(leftClass.methods)) COMPOSITION
    else AGGREGATION
  }
  val leftNote: String = "1"
  lazy val rightNote: String = {
    val tpt = field.fieldType
    if (tpt.isCollection) "0..*"
    else if (tpt.isOption) "0..1"
    else "1"
  }
  lazy val name: String = field.name
}

trait Modifier {
  val accessor: Accessor.Value
  val isStatic: Boolean
  val isAbstract: Boolean
  val isFinal: Boolean

  import Accessor._
  lazy val isPublic: Boolean = accessor == PUBLIC
  lazy val isProtected: Boolean = accessor == PROTECTED
  lazy val isPackage: Boolean = accessor == PACKAGE
  lazy val isPrivate: Boolean = accessor == PRIVATE

  def canShowByConfig(confShow: Modifier): Boolean = {
    if (confShow.accessor > accessor) false else List(
        Modifier.isStatic, Modifier.isAbstract, Modifier.isFinal
      ) forall (f => f(confShow) || !f(this))
  }
  lazy val toPublic: Modifier = {
    val outer = this
    if (isPublic) this
    else new Modifier {
      val accessor: Accessor = PUBLIC
      val isStatic: Boolean = outer.isStatic
      val isAbstract: Boolean = outer.isAbstract
      val isFinal: Boolean = outer.isFinal
    }
  }
}

object Modifier {
  val isPublic = (modifier: Modifier) => modifier isPublic
  val isProtected = (modifier: Modifier) => modifier isProtected
  val isPackage = (modifier: Modifier) => modifier isPackage
  val isPrivate = (modifier: Modifier) => modifier isPrivate
  val isStatic = (modifier: Modifier) => modifier isStatic
  val isAbstract = (modifier: Modifier) => modifier isAbstract
  val isFinal = (modifier: Modifier) => modifier isFinal
  
  val accessors = List(isPublic, isProtected, isPackage, isPrivate)
  val others = List(isStatic, isAbstract, isFinal)
  val all = accessors ::: others
  
  def createByAccessorName(accessorName: String): Modifier = {
    SimpleModifier(Accessor withName (accessorName toUpperCase))
  }
}
object PublicModifier extends Modifier {
  val accessor: Accessor = Accessor.PUBLIC
  val isStatic: Boolean = false
  val isAbstract: Boolean = false
  val isFinal: Boolean = false
}
case class SimpleModifier(
    accessor: Accessor = PRIVATE,
    isStatic: Boolean = true,
    isAbstract: Boolean = true, 
    isFinal: Boolean = true) extends Modifier {
  def this(accessorName: String) = this(Accessor withName (accessorName toUpperCase))
}

case class ExtraClass(isInterfaceOrNot: Boolean, name: String) extends ClassDesc {
  val classAttr: ClassAttr = if (isInterfaceOrNot) INTERFACE else CLASS
  val enumConstants: Traversable[String] = Nil
  val fields: Traversable[Field] = Nil
  val methods: Traversable[Method] = Nil
  val packageName: String = ""
  val fullName: String = ""
  val language: String = ""
  val extendTypes: Traversable[Type] = Nil
  val implementedTypes: Traversable[Type] = Nil
}

class ClassRelationFactory(uml: ClassDiagramUml, cls: ClassDesc) {
  def createRelations: Traversable[Relation] = {
    if (cls.name == "BusEventFilter") {
      null
    }
    val generalizations = cls.extendTypes map (_.simpleName) filterNot uml.isSkipClass map uml.findClassByName collect {
      case Some(superClass) => ClassRelation(superClass, EXTEND_BY, cls)
    }
    val realizations = cls.implementedTypes map (_.simpleName) filterNot uml.isSkipClass map uml.findClassByName collect {
      case Some(intf) => ClassRelation(intf, IMPLEMENT_BY, cls)
    }

    val aggregations = for {
      field <- cls.fields.filter(_.fieldType.isUmlClass)
      anotherClass = uml.findUmlClassByType(field.fieldType) if anotherClass.isDefined
    } yield ClassAggregate(cls, anotherClass.get, field)
    
    val dependencies = cls.methods.flatMap { method =>
      method.parameters.map(_.paramType) ++ method.returnType 
    }.toSet.map(uml.findUmlClassByType).collect {  // need to make Type hash able?
      case Some(anotherClasss) => ClassRelation(cls, DEPEND, anotherClasss)
    }
    generalizations ++ realizations ++ aggregations ++ dependencies
  }
}

class ComposableClassDiagram(umls: Traversable[ClassDiagramUml]) extends ClassDiagramUml {
  lazy val umlClasses: Traversable[ClassDesc] = umls flatMap (_.umlClasses)
  lazy val extraClasses: Traversable[ClassDesc] = umls flatMap (_.extraClasses)
  lazy val skipClasses: Set[String] = umls flatMap (_.skipClasses) toSet
  lazy val relations: Traversable[Relation] = umls flatMap (_.relations)
}

trait UmlExporter {
  def export(): Unit
}

class GroupUmlExporter(exporters: Traversable[UmlExporter]) extends UmlExporter {
  def export(): Unit = {
    exporters foreach (_ export)
  }
}

trait ExporterConfig {
  val name: String
  val exportToPlantUml: Option[Path]
  val exportToPNG: Option[Path]
  val exportToSVG: Option[Path]
  val printToConsole: Boolean
}

trait FilterConfig {
  val recursiveLevel: Int
  val supportedLanguages: Set[String]
  
  def supportLanguage(lang: String): Boolean = {
    supportedLanguages contains lang
  }
}
