package edu.knowitall.search.qa

import edu.knowitall.search.SearchAlgorithm
import com.google.common.collect.MinMaxPriorityQueue
import edu.knowitall.search.Node
import java.util.Comparator
import scala.collection.JavaConversions._

class QaLayeredSearch(override val problem: QaSearchProblem, beamSize: Int, goalSize: Int) extends SearchAlgorithm[QaState, QaAction] {
  
  private val comparator = new Comparator[Node[QaState, QaAction]] {
    override def compare(n1: Node[QaState, QaAction], n2: Node[QaState, QaAction]) = {
      val r = QaStateComparator.compare(n1.state, n2.state)
      if (r == 0) {
        n1.pathCost.compareTo(n2.pathCost)
      } else {
        r
      }
    }
  }
  
  private val beam = MinMaxPriorityQueue.orderedBy(comparator)
		  								.maximumSize(beamSize)
		  								.create[Node[QaState, QaAction]]
  
  override def continueSearch = (beam.size > 0) && (goals.size < goalSize)
  
  override def initialize = beam.add(rootNode)
  
  override def searchIter = {

    val node = beam.pollFirst
    logger.warn(s"Expanding ${node.state}")
    val successors = expand(node).toList
      
    val (newGoals, newNodes) = successors.partition(isGoal)
    newGoals.foreach(addGoalNode)
    beam.addAll(newNodes)

  }

}