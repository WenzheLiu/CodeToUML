package wenzhe.umlgen.seqdiagram

import java.nio.file.Path
import wenzhe.umlgen._
import java.nio.file.Files
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * @author wen-zhe.liu@asml.com
 *
 */
object SeqDiagramParser {
  val MethodCall = """(.*)\.(.+):(.+)\{""".r
  val MethodReturn = """\}(.*)\.(.+):(.+)""".r
  
  def parse(inputMethod: SeqMethod, files: Traversable[Path]): Seq[SequenceDiagram] = {
    files.flatMap(Files.lines(_).iterator()).collect {
      case MethodCall(pkg, cls, method) => (true, SeqMethod(SeqClass(pkg, cls), method))
      case MethodReturn(pkg, cls, method) => (false, SeqMethod(SeqClass(pkg, cls), method))
    }.foldLeft((Option.empty[Call], ArrayBuffer.empty[ArrayBuffer[SeqItem]])) { (result, pair) => 
      val (currentCallSite, items) = result
      pair match {
        case (true, method) => if (currentCallSite.isEmpty && inputMethod != method) result else {
            val call = Call(currentCallSite, method)
            if (currentCallSite.isEmpty) items += ArrayBuffer(call)
            else items.last += call
            (Some(call), items)
          }
        case (false, method) => if (!currentCallSite.exists(_.method == method)) result else {
            val call = currentCallSite.get
            items.last += Return(call)
            (currentCallSite.flatMap(_.caller), items)
          }
      }
    }._2.distinct.map(DefaultSeqDiagram(_))
  }
}