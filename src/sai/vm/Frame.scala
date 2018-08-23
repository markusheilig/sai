package vm

import cg.{PhantomObjectNode, _}
import org.apache.bcel.generic.ConstantPoolGen
import sai.bytecode.Method
import sai.vm.{LocalVars, OpStack, ObjectRef}

case class Frame(method: Method, cpg: ConstantPoolGen, stack: OpStack, localVars: LocalVars, cg: ConnectionGraph) {
}

object Frame {

  def apply(method: Method): Frame = {
    val (localVars, cg) = transferActuals(method)
    new Frame(method, method.cpg, OpStack(), localVars, cg)
  }

  private def transferActuals(method: Method) = {

    val actuals = method.inputReferences

    // create phantom nodes for actuals
    val phantomNodes = for {
      (_, ObjectRef(_, actual: ActualReferenceNode)) <- actuals
      phantom = PhantomObjectNode(actual)
    } yield phantom

    val phantomEscapes = for {
      phantom <- phantomNodes
    } yield phantom -> ArgEscape

    // create local nodes for actuals
    val localReferences = for {
      (index, ObjectRef(refType, actual: ActualReferenceNode)) <- actuals
      localReference = ObjectRef(refType, LocalReferenceNode(actual))
    } yield index -> localReference

    val localNodes = for {
      (_, ObjectRef(_, local)) <- localReferences
    } yield local

    val localEscapes = for {
      local <- localNodes
    } yield local -> NoEscape

    // link local nodes with phantom nodes
    val edges = for {
      (localNode: LocalReferenceNode, phantomNode: PhantomObjectNode) <- localNodes.zip(phantomNodes)
      edge = PointsToEdge(localNode -> phantomNode)
    } yield edge

    val localVars = LocalVars(method.maxLocals, localReferences)
    val cg = ConnectionGraph(localNodes.toSet ++ phantomNodes.toSet, edges.toSet, (localEscapes ++ phantomEscapes).toMap)
    (localVars, cg)
  }
}

