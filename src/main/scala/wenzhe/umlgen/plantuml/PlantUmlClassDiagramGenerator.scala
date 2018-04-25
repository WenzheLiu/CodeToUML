package wenzhe.umlgen.plantuml

import wenzhe.umlgen._

/**
 * @author wen-zhe.liu@asml.com
 *
 */
class PlantUmlClassDiagramGenerator(uml: ClassDiagramUml) extends UmlGenerator {
  def generate: String = s"""
@startuml
$printClasses
$printRelations
@enduml
""".lines filterNot (_.trim isEmpty) mkString "\n"
  
  private def printClasses: String = {
    uml.classes map printClass mkString "\n"
  }
  
  private def printClass(cls: ClassDesc): String = s"""
${printClassType(cls)} ${cls.name}${printTip(cls)}{
  ${printEnumConstants(cls.enumConstants)}
  ${printFields(cls.fields)}
  ${printMethods(cls.methods)}
}
""".trim()

  private def printEnumConstants(enumConstants: Traversable[String]): String = {
    enumConstants mkString "\n  "
  }
  
  private def printClassType(cls: ClassDesc): String = {
    if (cls isInterface) "interface"
    else if (cls isEnum) "enum"
    else if (cls isAbstractClass) "abstract class"
    else "class"
  }
  
  def printTip(cls: ClassDesc): String = {
    cls.tip map(" [[" + _ + "]] ") getOrElse " "
  }
  
  private def printFields(fields: Traversable[Field]): String = {
    fields map printField mkString "\n  "
  }
  
  private def printField(field: Field): String = {
    val fieldType = field.fieldType.name
    s"${printModifier(field.modifier)} ${field.name}" + {
      if (fieldType.isEmpty) ""
      else ": " + fieldType
    }
  }
  
  private val modifierToString = {
    import Modifier._
    Map(
        isPublic -> "+",
        isProtected -> "#",
        isPackage -> "~",
        isPrivate -> "-",
        isStatic -> "{static}",
        isAbstract -> "{abstract}",
        isFinal -> ""  //TODO: can be final or not in plant uml?
        )
  }
  
  private def printModifier(modifier: Modifier): String = {
    Modifier.all filter (_(modifier)) map modifierToString mkString ""
  }
  
  private def printMethods(methods: Traversable[Method]): String = {
    methods map printMethod mkString "\n  "
  }
  
  private def printMethod(m: Method): String = {
    s"${printModifier(m.modifier)} ${printMethodName(m)}(${printParams(m.parameters)})${printReturnType(m.returnType)}"
  }
  
  private def printMethodName(m: Method): String = {
    if (m.isConstructor) "<init>" else m.name
  }
  
  private def printReturnType(returnType: Option[Type]): String = {
    returnType map (": " + _.name) getOrElse ""
  }
  
  private def printParams(ps: Traversable[Parameter]): String = {
    ps map printParam mkString ", "
  }
  
  private def printParam(p: Parameter): String = {
    s"${p.name}: ${p.paramType.name}"
  }
  
  private def printRelations: String = {
    uml.relations map printRelation mkString "\n"
  }
  
  private def printRelation(r: Relation): String = {
    r match {
      case n: RelationNote => 
        //val leftNote = quoteIfNotEmpty(n.leftNote)
        val rightNote = quoteIfNotEmpty(n.rightNote)
        //s"${n.leftClass} ${leftNote} ${printRelationType(n.relationType)} ${rightNote} ${n.rightClass}: ${n.name}"
        s"${n.leftClass.name} ${printRelationType(n.relationType)} ${rightNote} ${n.rightClass.name}: ${n.name}"
      
      case _ => s"${r.leftClass.name} ${printRelationType(r.relationType)} ${r.rightClass.name}"
    }
  }
  
  private def quoteIfNotEmpty(s: String) = if (s.isEmpty()) "" else "\"" + s + "\""
  
  private val relationTypeToString = {
    import RelationType._
    Map(
        DEPEND -> "..>",
        ASSOCIATION -> "--",
    	  AGGREGATION -> "o--",
        COMPOSITION -> "*--",
        EXTEND_BY -> "<|--",
        IMPLEMENT_BY -> "<|.."
        )
  }
  
  private def printRelationType(t: RelationType.Value): String = {
    relationTypeToString.apply(t)
  }
}
