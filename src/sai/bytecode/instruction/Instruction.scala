package sai.bytecode.instruction

import org.apache.bcel.generic.ConstantPoolGen
import sai.bytecode.Method
import sai.vm.State

class Instruction(bcelInstruction: org.apache.bcel.generic.InstructionHandle, cpg: ConstantPoolGen, 
    method: Method) {
 
 
  def predecessors: Set[Instruction] = Set()
    
  def statesIn: Set[State] = 
    for (predecessor <- predecessors;
        predState <- predecessor.statesOut) yield predState
  
  private def transfer(states: Set[State]) = states 
 
  def statesOut = transfer(statesIn) 
  
  override def toString = bcelInstruction.getInstruction.getName
  
  def print {
    println("-" + toString)
  }
}

object Instruction {
  def apply(bcelInstruction : org.apache.bcel.generic.InstructionHandle, cpg: ConstantPoolGen, method: Method) = 
    bcelInstruction getInstruction match {
      case bi: org.apache.bcel.generic.BranchInstruction => new ControlFlowInstruction(bcelInstruction, cpg, method)
      case _ => new Instruction(bcelInstruction, cpg, method)
    }
    
    
}
