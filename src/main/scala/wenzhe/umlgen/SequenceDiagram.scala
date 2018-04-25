package wenzhe.umlgen

/**
 * @author wen-zhe.liu@asml.com
 *
 */
trait SeqItem
case class SeqClass(pkg: String, name: String) {
  lazy val simpleName: String = name.split('$').takeWhile(_ != "class").last
}
case class SeqMethod(cls: SeqClass, name: String)
case class Call(caller: Option[Call], method: SeqMethod) extends SeqItem
case class Return(call: Call) extends SeqItem
trait SequenceDiagram {
  def items: Traversable[SeqItem]
}
case class DefaultSeqDiagram(items: Traversable[SeqItem]) extends SequenceDiagram