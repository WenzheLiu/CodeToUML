package wenzhe.umlgen.plantuml
import wenzhe.umlgen._
import wenzhe.umlgen.UmlGenerator

class SequenceDiagramGenerator(seqDiagram: SequenceDiagram) extends UmlGenerator {
  def generate: String = s"""
@startuml
$printSequenceDiagram
@enduml
"""
  def printSequenceDiagram: String = {
    seqDiagram.items.collect {
      case Call(caller, method) => s"${caller.map(_.method.cls.simpleName).getOrElse("client")} -> ${method.cls.simpleName}: ${method.name}\nactivate ${method.cls.simpleName}"
      case Return(call) => s"deactivate ${call.method.cls.simpleName}"
    }.mkString("\n")
  }
}
