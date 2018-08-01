package cg

import scala.annotation.tailrec
import scala.collection.immutable.Set

import cg.EscapeSet.EscapeSet
import cg.EscapeStates.EscapeState

case class ConnectionGraph(nodes: Set[Node], edges: Set[Edge], escapeSet: EscapeSet) {

  /**
   * Merge connection graph with another connection graph.
   *
   * @param other Connection graph to merge.
   * @return A new connection graph which represents the merge.
   */
  def merge(other: ConnectionGraph): ConnectionGraph = {
    val newNodes = nodes.union(other.nodes)
    val newEdges = edges.union(other.edges)
    val newEscapeSet = mergeEscapeSets(other)
    ConnectionGraph(newNodes, newEdges, newEscapeSet)
  }

  private def mergeEscapeSets(other: ConnectionGraph): EscapeSet = {
    val N1 = this.nodes
    val N2 = other.nodes
    val N3 = N1.union(N2)

    (for {
      n3 <- N3
      n1 = N1.find(n1 => n1.id == n3.id)
      n2 = N2.find(n2 => n2.id == n3.id)
      n3es = (n1, n2) match {
          case (Some(node1), Some(node2)) =>
            EscapeStates.merge(this.escapeSet(node1), other.escapeSet(node2))
          case (Some(node1), _) =>
            this.escapeSet(node1)
          case (_, Some(node2)) =>
            other.escapeSet(node2)
        }
    } yield (n3, n3es)).toMap

  }

  /**
    * Add a variable number of nodes to the CG.
    *
    * @param nodes variable argument list of nodes.
    * @return A connection graph with the added nodes.
    */
  def addNodes(nodes: Node*): ConnectionGraph = {
    copy(nodes = this.nodes ++ nodes)
  }

  def addNodes(nodes: Set[_ <: Node]): ConnectionGraph = {
    copy(nodes = this.nodes ++ nodes)
  }

  /**
    * Add a node to the CG.
   *
   * @param node Node to add to the graph.
   * @return A connection graph with the added node.
   */
  def addNode(node: Node): ConnectionGraph = {
      copy(nodes = nodes + node)
    }

  /**
    * Add an edge to the connection graph.
    *
    * @param fromTo Edge to add to the graph.
    * @return A connection graph with the added edge.
    */
  def addEdge(fromTo: (Node, Node)): ConnectionGraph = {
    val edge = fromTo match {
      case (local: LocalReferenceNode, obj: ObjectNode) => PointsToEdge(local -> obj)
      case (obj: ObjectNode, field: FieldReferenceNode) => FieldEdge(obj -> field)
      case (ref1: ReferenceNode, ref2: ReferenceNode) => DeferredEdge(ref1 -> ref2)
      case (from, to) => throw new IllegalArgumentException(s"cannot create an edge for types ${from.getClass.getSimpleName} -> ${to.getClass.getSimpleName}")
    }
    addEdge(edge)
  }

  /**
    * Add an edge to the connection graph.
   *
   * @param edge Edge to add to the graph.
   * @return A connection graph with the added edge.
   */
  def addEdge(edge: Edge): ConnectionGraph = {
      copy(edges = edges + edge)
    }

  def addEdges(edges: Set[Edge]): ConnectionGraph = {
    copy(edges = this.edges ++ edges)
  }

  /**
   * Kill local variable (i.e. bypass ingoing/outgoing edges).
   *
   * @param p local reference node to kill.
   * @return A connection graph with the localReferenceNode bypassed.
   */
  def byPass(p: LocalReferenceNode): ConnectionGraph = {
    val ingoingDeferredEdges  = edges.collect {
      case edge @ DeferredEdge(_, `p`) => edge
    }
    val outgoingPointsToEdges = edges.collect {
      case edge @ PointsToEdge(`p`, _) => edge
    }
    val outgoingDeferredEdges = edges.collect {
      case edge @ DeferredEdge(`p`, _) => edge
    }

    val bypassedPointsToEdges =
      for {
        in <- ingoingDeferredEdges
        out <- outgoingPointsToEdges
      } yield PointsToEdge(in.from -> out.to)

    val bypassedDeferredEdges =
      for {
        in <- ingoingDeferredEdges
        out <- outgoingDeferredEdges
      } yield DeferredEdge(in.from -> out.to)

    val edgesToRemove = ingoingDeferredEdges ++ outgoingPointsToEdges ++ outgoingDeferredEdges
    val edgesToAdd = bypassedPointsToEdges ++ bypassedDeferredEdges

    copy(edges = edges -- edgesToRemove ++ edgesToAdd)
  }

  /**
   * Find all object nodes with a points-to path of length one.
   *
   * @param node Node for which the points-to analysis will be performed.
   * @return A set of object nodes that are connected to <code>node</code> with exactly one points-to path.
   */
  def pointsTo(node: ReferenceNode): Set[ObjectNode] = {

    @tailrec
    def pointsToRec(nodes: List[ReferenceNode], objects: Set[ObjectNode]): Set[ObjectNode] = nodes match {
      case Nil => objects
      case m :: tail =>
        val pointsTo = edges.collect {
          case PointsToEdge(`m`, n) => n
        }
        val deferred = edges.collect {
          case DeferredEdge(`m`, to) => to
        }
        pointsToRec(tail ++ deferred, objects ++ pointsTo)
    }

    pointsToRec(List(node), objects = Set())
  }

  /**
   * Check if a node is a 'terminal node'.
   * A node is called a 'terminal node' if it has no outgoing (points-to) edges.
   *
   * @param node Node to check.
   * @return true if node is a terminal node, false otherwise.
   */
  private def isTerminalNode(node: ReferenceNode) = pointsTo(node).isEmpty

  override def toString: String = s"Nodes: \n\t${nodes.mkString("\n\t")}\nEdges: \n\t${edges.mkString("\n\t")}\nEscapeSet: \n\t${escapeSet.mkString("\n\t")}"

}

object ConnectionGraph {
  def empty(): ConnectionGraph = new ConnectionGraph(Set(), Set(), EscapeSet())
}
