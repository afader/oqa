package edu.knowitall.search

import scala.collection.mutable.{Set => MutableSet}
import scala.collection.mutable.{Map => MutableMap}
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

class BeamSearch[State, Action](
    override val problem: SearchProblem[State, Action],
    beam: Beam[State, Action],
    goalSize: Int = BeamSearch.defaultGoalSize,
    maxIters: Int = BeamSearch.defaultMaxIters,
    expandPerIter: Int = BeamSearch.defaultExpandPerIter) 
    extends SearchAlgorithm[State, Action] {
  
  assert(goalSize >= 1)
  
  override val logger = LoggerFactory.getLogger(this.getClass)
  
  override def continueSearch = (goals.size < goalSize) && (!beam.isEmpty) && (iterNum < maxIters)
  
  override def searchIter = {
    val initialSize = beam.size 
        
    logger.debug("Expanding frontier")
    val (toExpand, toKeep) = beam.splitAt(expandPerIter)
    
    toExpand foreach { n => logger.debug(s"Chose to expand: ${n.pathCost} ${n.state}") }
    
    val newNodes = toExpand.par.flatMap(expand).toList
    logger.debug(s"Expanded to ${newNodes.size} new nodes")
      
    logger.debug("Adding goal nodes")
    newNodes.filter(isGoal).foreach(addGoalNode)
      
    logger.debug("Updating new frontier")
    beam.setNodes(newNodes.filter(n => !isGoal(n) && !haveExpanded(n)) ++ toKeep)
      
    val numGoals = newNodes.count(isGoal(_))
    logger.debug(s"Done with search iteration $iterNum")
    logger.debug(s"Initial frontier size = $initialSize")
    logger.debug(s"Final frontier size = ${beam.size}")
    logger.debug(s"Expanded to ${newNodes.size} new nodes")
    logger.debug(s"Found $numGoals new goal nodes")
    
  }
  
  override def initialize = beam.setNodes(rootNode)

}

object BeamSearch {
  val conf = ConfigFactory.load()
  val defaultMaxIters = conf.getInt("search.maxIters")
  val defaultGoalSize = conf.getInt("search.goalSize")
  val defaultExpandPerIter = conf.getInt("search.expandPerIter")
}