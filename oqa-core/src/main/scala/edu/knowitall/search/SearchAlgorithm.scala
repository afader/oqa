package edu.knowitall.search

import org.slf4j.LoggerFactory
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.{Set => MutableSet}
import edu.knowitall.util.TimingUtils
import com.typesafe.config.ConfigFactory

case class Node[State, Action](
    state: State, 
    parent: Option[Edge[State, Action]],
    pathCost: Double,
    creationTime: Long) extends Comparable[Node[State, Action]] {
  
  def this(state: State, parent: Option[Edge[State, Action]], pathCost: Double) = 
    this(state, parent, pathCost, System.currentTimeMillis) 
  
  def path(rest: List[(State, Action, State)] = Nil): List[(State, Action, State)] = parent match {
    case None => rest
    case Some(e) => e.node.path((e.node.state, e.action, state) :: rest)
  }
  
  def compareTo(that: Node[State, Action]) = this.pathCost.compareTo(that.pathCost)
  
}

case class Edge[State, Action](action: Action, node: Node[State, Action])

abstract class SearchAlgorithm[State, Action] {
  
  val logger = LoggerFactory.getLogger(this.getClass)

  def problem: SearchProblem[State, Action]
  
  def rootNode: Node[State, Action] = new Node(problem.initialState, None, 0.0)
  
  def isGoal(n: Node[State, Action]) = problem.isGoal(n.state)
  
  private var iter = 0
  
  protected def iterNum = iter
  
  protected val goals = MutableMap.empty[State, Node[State, Action]]
  
  protected def addGoalNode(node: Node[State, Action]) = {
    assert(isGoal(node))
    val state = node.state
    val otherNode = goals.getOrElse(state, node)
    val bestNode = List(node, otherNode).minBy(_.pathCost)
    goals.put(state, bestNode)
  }
  
  protected val expanded = MutableSet.empty[State]
  
  protected def markExpanded(n: Node[State, Action]) = expanded.add(n.state)
  
  protected def haveExpanded(n: Node[State, Action]) = expanded.contains(n.state)
  
  def expand(node: Node[State, Action]) = {
    for {
      (action, nextState) <- problem.successors(node.state)
      cost = node.pathCost + problem.cost(node.state, action, nextState)
      edge = Edge(action, node)
    } yield {
      markExpanded(node)
      new Node(nextState, Some(edge), cost)
    }
  }
  
  def searchIter: Unit
  
  def continueSearch: Boolean
  
  def initialize(): Unit
  
  private var timedOut = false
  
  private var t0 = System.currentTimeMillis
  
  def startTime = t0
  
  private def runSearch = {
    initialize()
    do {
      searchIter
      iter += 1
    } while (continueSearch && !timedOut)    
  }
  
  def search = {
    t0 = System.currentTimeMillis
    val time = SearchAlgorithm.defaultMaxSearchTimeSec * 1000
    TimingUtils.runWithTimeout(time) { runSearch }
    timedOut = true
    goals.values.toList.distinct.sortBy(_.pathCost)
  }
}

object SearchAlgorithm {
  val conf = ConfigFactory.load()
  val defaultMaxSearchTimeSec = conf.getLong("search.maxSearchTimeSec") 
}