package edu.knowitall.search

trait Beam[State, Action] {
  def nodes: Iterable[Node[State, Action]]
  def splitAt(k: Int): (Iterable[Node[State, Action]], Iterable[Node[State, Action]])
  def setNodes(nodes: Iterable[Node[State, Action]]): Unit
  def setNodes(nodes: Node[State, Action]*): Unit = setNodes(nodes)
  def size: Int
  def isEmpty = size == 0
}

object Beam {
  def distinctByState[State, Action](nodes: Iterable[Node[State, Action]]) = 
    nodes.groupBy(_.state) map {
      case (state, group) => group.minBy(_.pathCost)
    }
}

class SingleBeam[State, Action](beamSize: Int) extends Beam[State, Action] {
  def this(beamSize: Int, nodes: Iterable[Node[State, Action]]) = {
    this(beamSize)
    setNodes(nodes)
  }
  private var beam = List.empty[Node[State, Action]]
  override def nodes = beam
  override def splitAt(k: Int) = beam.splitAt(k)
  override def setNodes(nodes: Iterable[Node[State, Action]]) = {
    beam = Beam.distinctByState(nodes).toList.sortBy(_.pathCost).take(beamSize)
  }
  override def size = beam.size
}

class TypedBeams[State, Action, T](f: Node[State, Action] => T, beamSize: Int) extends Beam[State, Action] {
  private var beams = Map.empty[T, SingleBeam[State, Action]]
  override def nodes = beams.values.flatMap(_.nodes).toList.sortBy(_.pathCost)
  override def splitAt(k: Int) = {
   val listOfNodes = beams.values.map(_.nodes).toList
   val orderedNodes = transpose(listOfNodes).flatten
   orderedNodes.splitAt(k)
  }
  override def setNodes(nodes: Iterable[Node[State, Action]]) = {
    val distinct = Beam.distinctByState(nodes)
    val grouped = distinct.groupBy(f)
    beams = for ((t, typed) <- grouped; beam = new SingleBeam(beamSize, typed)) yield (t -> beam)
  }
  override def size = beams.values.map(_.size).sum
  
  private def transpose[T](xss: List[List[T]]): List[List[T]] = xss.filter(!_.isEmpty) match {    
    case Nil => Nil
    case ys: List[List[T]] => ys.map{ _.head }::transpose(ys.map{ _.tail })
  }
}