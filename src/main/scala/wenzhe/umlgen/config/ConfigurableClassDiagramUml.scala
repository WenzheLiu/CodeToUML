package wenzhe.umlgen.config

import wenzhe.umlgen._
import RelationType._
import ClassAttr._
import scala.annotation.tailrec

/**
 * @author wen-zhe.liu@asml.com
 *
 */
class ConfigurableClassDiagramUml(conf: ClassDiagramConfig, uml: ClassDiagramUml) extends ClassDiagramUml {
  override lazy val classes: Traversable[ClassDesc] = new Transformer(
    super.classes
  ).mapIf (!conf.centerClass.isEmpty && conf.maxDistanceToCenter >= 0) { clses =>
    analysisNeighbors(uml.relations.toSeq) match {
      case (_, visitedClasses) => clses filter (visitedClasses contains _.name)
    }
  }.value
  lazy val extraClasses: Traversable[ClassDesc] = {
    if (conf.showOtherClassAsBaseClass) uml.extraClasses
    else Nil
  }
  lazy val umlClasses: Traversable[ClassDesc] = {
    uml.umlClasses map (new ConfigurableClassDesc(_))
  }
  lazy val skipClasses: Set[String] = uml skipClasses
  lazy val relations: Traversable[Relation] = new Transformer(uml.relations collect {
      case n: Relation with RelationNote => new ConfigurableRelationWithNote(n)
      case r => new ConfigurableRelation(r)
    }).mapIf (!conf.centerClass.isEmpty && conf.maxDistanceToCenter >= 0) { rs =>
      val umlRelations = rs.toList
      analysisNeighbors(umlRelations) match {
        case (remainRelations, _) => umlRelations diff remainRelations
      }

    }.mapIf (!conf.showOtherClassAsBaseClass) {
      _ filterNot { r =>
        Seq(EXTEND_BY, IMPLEMENT_BY).contains(r.relationType) && 
        Seq(r.leftClass, r.rightClass).map(_.name).exists (!isUmlClass(_))
      }
    }.mapIf (!conf.showExtensions) {
      _ filterNot (_.relationType == EXTEND_BY)
    }.mapIf (!conf.showImplementations) {
      _ filterNot (_.relationType == IMPLEMENT_BY)
    }.mapIf (!conf.showAggregations) {
      _ filterNot (_.relationType == AGGREGATION)
    }.mapIf (!conf.showCompositions) {
      _ filterNot (_.relationType == COMPOSITION)
    }.mapIf (!conf.showDependencies) {
      _ filterNot (_.relationType == DEPEND)
    }.mapIf (!conf.showDependencyToSelf) {
      _ filterNot { r =>
        r.relationType == DEPEND && r.isSelfRelation
      }
    }.mapIf (conf.removeDependencyIfExtendOrAggregate) {
      _.groupBy { relation => 
        Set(relation.leftClass.name, relation.rightClass.name)
      }.values.map(_.toList).map { umlRelations =>
        lazy val aggregations = umlRelations filter (_.relationType == AGGREGATION)
        lazy val extendOrImplements = umlRelations filter {
          List(EXTEND_BY, IMPLEMENT_BY) contains _.relationType
        }
        umlRelations filterNot { relation => relation.relationType == DEPEND && (
          aggregations.exists (_.isSameDirectionWith(relation)) ||
          extendOrImplements.exists (_.isReverseDirectionWith(relation))
        )}
      }.flatten
    }.value
    
  private def analysisNeighbors(umlRelations: Seq[Relation]): (Seq[Relation], Set[String]) = {
    @tailrec def analysisNeighborsRecursively(distanceToTarget: Int, allRelations: Seq[Relation], 
        visitedClasses: Set[String], centerClasses: Set[String]): (Seq[Relation], Set[String]) = {
      if (distanceToTarget <= 0) (allRelations, visitedClasses)
      else {
        val (remainRelations, allNeighberClasses) = ((allRelations, Set[String]()) /: centerClasses) {
          //the above /: operator is the same with: centerClasses.foldLeft((allRelations, Set[String]())) { 
          case ((remainRelations, allNeighberClasses), centerClass) =>
            val (directRelations, otherRelations) = remainRelations partition { r =>
              List(r.leftClass.name, r.rightClass.name) contains centerClass
            }
            val neighberClasses = directRelations.map { r => 
              if (r.leftClass.name != centerClass) r.leftClass else r.rightClass
            }.map(_.name).toSet
            (otherRelations, allNeighberClasses ++ neighberClasses)
        }
        analysisNeighborsRecursively(distanceToTarget - 1, remainRelations, 
            visitedClasses ++ allNeighberClasses, allNeighberClasses)
      }
    }
    val centerClasses = Set(conf.centerClass)
    analysisNeighborsRecursively(conf.maxDistanceToCenter, umlRelations, centerClasses, centerClasses)
  }
  
  // no used, replaced by analysisNeighborsRecursively
  private def analysisNeighborsByLoop(umlRelations: Seq[Relation]): (Seq[Relation], Set[String]) = {
    var centerClasses = Set(conf.centerClass)
    var remainRelations = umlRelations
    var visitedClasses = centerClasses
    for (i <- 1 to conf.maxDistanceToCenter) {
      var newVisitedClasses: Set[String] = Set()
      for (centerClass <- centerClasses) {
        val (directRelations, otherRelations) = remainRelations.partition { r =>
          List(r.leftClass.name, r.rightClass.name) contains centerClass
        }
        val neighberClasses = directRelations.map { r => 
          if (r.leftClass.name != centerClass) r.leftClass else r.rightClass
        }.map(_.name).toSet
        remainRelations = otherRelations
        newVisitedClasses = newVisitedClasses ++ neighberClasses
      }
      centerClasses = centerClasses ++ newVisitedClasses
      visitedClasses = visitedClasses ++ newVisitedClasses
    }
    (remainRelations, visitedClasses)
  }
  
  class ConfigurableClassDesc(classDesc: ClassDesc) extends ClassDesc {
    lazy val classAttr: ClassAttr = classDesc classAttr
    lazy val name: String = {
      if (conf.showPackage) Some(classDesc.packageName) filterNot (_.isEmpty) map (_ + ".") getOrElse ""
      else ""
    } + classDesc.name
    
    lazy val enumConstants: Traversable[String] = classDesc.enumConstants
    lazy val fields: Traversable[Field] = if (!conf.showField) Nil else {
      new Transformer(classDesc.fields.filter {
        _.modifier canShowByConfig conf.leastFieldModifier
      }).mapIf (!conf.showStaticField) {
        _ filterNot ( _.modifier.isStatic )
      }.mapIfMatch (conf.sortField) {
        case it: Seq[Field] => it sortBy (_.name)
      }.mapIf (conf.getterSetterToPublicField) { fields =>
        val beans = new BeanProvider(classDesc).beans.toSet
        fields map { field => 
          if (beans contains field.name) field toPublic else field
        }
      }.mapIf (conf.removeFieldIfRelateToOther) {
        _ filterNot (_.fieldType.isUmlClass)
      }.value
    }
    private lazy val canShowMethods: Boolean = {
      if (!conf.showMethod) false
      else if (conf.onlyShowMethodInInterface) classDesc isInterface
      else true
    }
    lazy val methods: Traversable[Method] = if (!canShowMethods) Nil else {
      new Transformer(classDesc.methods.filterNot {
        _.isConstructor && !conf.showConstructor
      }.filter {
        _.modifier canShowByConfig conf.leastMethodModifier
      }).mapIf (!conf.showStaticMethod) {
        _ filterNot ( _.modifier.isStatic )
      }.mapIfMatch (conf.sortMethod) {
        case it: Seq[Method] => it sortBy (_.name)
      }.mapIfMatch (conf.showConstructorFirst) {
        case it: Seq[Method] => it sortBy (!_.isConstructor)
      }.mapIf (conf.getterSetterToPublicField) {
        val beans = new BeanProvider(classDesc).beans.toSet
        _ filterNot (_ match {
          case method: MayBeBean => method.bean map (beans contains _) getOrElse false
          case _ => false
        })
      }.value
    }
    lazy val packageName: String = classDesc packageName
    lazy val fullName: String = if (conf generateFullName) classDesc fullName else ""
    lazy val language: String = classDesc language
    lazy val extendTypes: Traversable[Type] = classDesc extendTypes
    lazy val implementedTypes: Traversable[Type] = classDesc implementedTypes
  }
  
  class ConfigurableRelation(relation: Relation) extends Relation {
    lazy val leftClass: ClassDesc = new ConfigurableClassDesc(relation.leftClass)
    lazy val relationType: RelationType = relation.relationType
    lazy val rightClass: ClassDesc = new ConfigurableClassDesc(relation.rightClass)
  }
  
  class ConfigurableRelationWithNote(relation: Relation with RelationNote) 
      extends ConfigurableRelation(relation) with RelationNote {
    lazy val leftNote: String = relation.leftNote
    lazy val rightNote: String = relation.rightNote
    lazy val name: String = relation.name
  }
}

class Transformer[T](val value: T) {
  def map(f: T => T) = new Transformer(f(value))
  def map(pf: PartialFunction[T, T]): Transformer[T] = {
    if (pf.isDefinedAt(value)) new Transformer(pf(value)) else this
  }
  def mapIf(condition: T => Boolean)(f: T => T): Transformer[T] = {
    mapIf(condition(value)) { f }
  }
  def mapIf(condition: => Boolean)(f: T => T): Transformer[T] = {
    if (!condition) this else map(f)
  }
  def mapIfMatch(condition: T => Boolean)(pf: PartialFunction[T, T]): Transformer[T] = {
    mapIf(condition(value)) { pf }
  }
  def mapIfMatch(condition: => Boolean)(pf: PartialFunction[T, T]): Transformer[T] = {
    if (!condition) this else map(pf)
  }
}
