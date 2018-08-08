package vm.interpreter.impl

import cg.{ReferenceNode, StaticReferenceNode}
import org.apache.bcel.generic.{PUTSTATIC, ReferenceType}
import sai.vm.Reference
import sai.vm.Reference.Null
import vm.Frame
import vm.interpreter.InstructionInterpreter
import cg.GlobalEscape
import vm.interpreter.InstructionInterpreter.Interpreter

private[interpreter] object PutStaticInterpreter extends InstructionInterpreter[PUTSTATIC] {

  override def apply(i: PUTSTATIC): Interpreter = {
    case frame@Frame(_, cpg, stack, _, cg) =>
      val value = stack.peek
      val updatedStack = stack.pop
      i.getReferenceType(cpg) match {
        case referenceType: ReferenceType =>
          value match {
            case Null =>
              frame.copy(stack = updatedStack)
            case Reference(_, q: ReferenceNode) =>
              val staticReferenceNode = StaticReferenceNode(referenceType, i.getIndex)
              val updatedCG = cg
                .addNode(staticReferenceNode)
                .addEdge(staticReferenceNode -> q)
                .updateEscapeState(staticReferenceNode -> GlobalEscape)
              frame.copy(stack = updatedStack, cg = updatedCG)
          }
        case _ =>
          frame.copy(stack = updatedStack)
      }
  }
}
