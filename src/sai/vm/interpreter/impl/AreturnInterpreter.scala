package vm.interpreter.impl

import ea.{ArgEscape, PhantomReturnNode}
import org.apache.bcel.generic.ARETURN
import sai.vm.{OpStack, Reference}
import vm.Frame
import vm.interpreter.{InstructionInterpreter, InterpreterBuilder}

private[interpreter] object AreturnInterpreter extends InterpreterBuilder[ARETURN] {

  override def apply(i: ARETURN): InstructionInterpreter = {
    case frame@Frame(method, _, stack, _, cg, _) =>
      val returnNode = new PhantomReturnNode(method.id)
      var updatedCG =
        cg.addNode(returnNode)
          .updateEscapeState(returnNode -> ArgEscape)
      updatedCG = stack.peek match {
        case _@Reference(_, node) =>
          updatedCG.addEdge(returnNode -> node)
        case _ =>
          updatedCG
      }
      frame.copy(stack = OpStack(), cg = updatedCG)
  }

}
