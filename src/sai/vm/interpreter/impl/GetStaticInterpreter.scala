package vm.interpreter.impl

import ea.{GlobalEscape, StaticReferenceNode}
import org.apache.bcel.generic.{BasicType, GETSTATIC, ReferenceType}
import sai.vm.{DontCare, Reference}
import vm.Frame
import vm.interpreter.InterpreterBuilder
import vm.interpreter.InstructionInterpreter

private[interpreter] object GetStaticInterpreter extends InterpreterBuilder[GETSTATIC] {

  override def apply(i: GETSTATIC): InstructionInterpreter = {
    case frame @ Frame(_, cpg, stack, _, cg, _) =>
      i.getFieldType(cpg) match {
        case referenceType: ReferenceType =>
          val staticReferenceNode = StaticReferenceNode(referenceType, i.getFieldName(cpg))
          val reference           = Reference(referenceType, staticReferenceNode)
          val updatedStack        = stack.push(reference)
          val updatedCG =
            cg.addNode(staticReferenceNode)
              .updateEscapeState(staticReferenceNode -> GlobalEscape)
          frame.copy(stack = updatedStack, cg = updatedCG)
        case _: BasicType =>
          val updatedStack = stack.push(DontCare, i.produceStack(cpg))
          frame.copy(stack = updatedStack)
      }
  }
}
