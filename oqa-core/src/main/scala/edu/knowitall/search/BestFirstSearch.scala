package edu.knowitall.search

import scala.collection.mutable.{Set => MutableSet}
import scala.collection.mutable.{Map => MutableMap}
import com.google.common.collect.MinMaxPriorityQueue
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory

class BestFirstSearch[State, Action](override val problem: SearchProblem[State, Action], beamSize: Int, goalSize: Int) extends SearchAlgorithm[State, Action] {
  
  override val logger = LoggerFactory.getLogger(this.getClass)
  
  private val beam = MinMaxPriorityQueue.maximumSize(beamSize).create[Node[State, Action]]()
  
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