package edu.knowitall.execution
import Search.PartialSearchJoin
import PartialFunction._
import Search.Field
import Conditions.TuplePred
import Operators.{Select, Product}
import Conditions.AttrsSim
import Operators.NestedLoopJoin
import org.slf4j.LoggerFactory
import edu.knowitall.triplestore.TriplestoreClient
import edu.knowitall.triplestore.TriplestorePlan
import scala.Option.option2Iterable

/**
 * A Joiner object interfaces with a triplestore and executes a query 
 * against it. It uses a simple query plan optimizer to take a set of 
 * TConjunct objects and join them appropriately. It heuristically
 * avoids joining large tables together by (1) limiting the number of rows
 * returned by the triplestore, and (2) converting join(small table, large 
 * table) calls into a set of join(small table, small table) calls.
 * 
 * The algorithm first picks a variable that appears in the TConjunct
 * objects. It then eliminates that variable by executing the TConjunct
 * and enforcing the join constraints between them. 
 * 
 * For example, suppose the set of queries is aq1 = ($x, type, us president),
 * aq2 = ($x, born in, $y), and aq3 = ($y, type, us state). The joiner first
 * picks a variable to eliminate, using a herustic; say it picks $x. Then, 
 * the joiner takes all queries that have $x as a value, loads their data,
 * and joins them. In this case, it must load the data for aq1 and aq2, then
 * join them together on the constraint aq1.arg1 = aq2.arg1. It saves the
 * results as an intermediate table t. Then, it repeats, picking $y as the
 * next variable to eliminate, and joining intermediate table t with aq3. 
 * 
 * The heuristic used to estimate the cost of a query is the number of rows
 * returned by the triplestore.
 * 
 * Internally, the Joiner uses two structures. The first is a QueryNode, which
 * represents an unexecuted TConjunct. The second is a TuplesNode, which
 * represents an intermediate table (a set of AbstractQueries that have been
 * executed and joined across a single variable). The QueryNode class and
 * TuplesNode class are subclasses of TableNode.
 * 
 */
case class Joiner(client: TriplestoreClient) {
  
  val logger = LoggerFactory.getLogger(this.getClass)
  
  val maxJoinHits = 10
  
  /* Import some query plan objects from the client. */
  val planning = TriplestorePlan(client)
  import planning._
  
  /* Runs the join algorithm on the given set of AbstractQuery objects.*/
  def joinQueries(conjs: Iterable[TConjunct]): Tuples = {
    val nodes = conjs.map(QueryNode(_)).toList
    join(nodes)
  }
 
  /* Any table T joined with the emptyTuples set will return T. */ 
  val emptyTuples = List(Tuple(Map.empty))
  
  /* Takes the cartesian product of two tuple sets. */
  val prod = (ts1: Iterable[Tuple], ts2: Iterable[Tuple]) => 
    Product(ts1, ts2).toList
    
  /* Joins the given TableNodes together. If the end result is not a single
   * TableNode object, then just takes the cartesian product of the remaining
   * nodes.
   */  
  def join(nodes: List[TableNode]): Iterable[Tuple] = {
    val joined = mergeLowest(nodes).map(toTuplesNode(_))
    val cleaned = joined map eliminateStrandedVars
    cleaned.map(_.tuples).foldLeft(emptyTuples)(prod).toList
  }
   
  /*
   * Eliminates any join variables that were not eliminated 
   * during the join process - for example,
   * (salad, $r, $x) (beef, $r, $x) will leave $r stranded 
   * in a single TuplesNode at the end of join processing,
   * but still referring to r0.rel and r1.rel. This method
   * enforces the join condition within a single TuplesNode.
   */
  def eliminateStrandedVars(tn: TuplesNode): TuplesNode = {
    
    tn.joinAttrs.find(_._2.size > 1) match {
      case Some((v, attrs)) => {
        val tuples = tn.tuples.filter { t =>
          attrs.sliding(2).forall { 
            case List(a1, a2) => AttrsSim(a1, a2, 0.9).apply(t)
            case _ => throw new RuntimeException()
          }
        }
        val newTuplesNode = TuplesNode(tuples, tn.joinAttrs-v)
        eliminateStrandedVars(newTuplesNode)
      }
      case None => tn
    }
  }
    
  /* Picks the lowest-cost variable, merges the nodes, and then repeats until
   * there are no variables left to merge.
   */
  def mergeLowest(nodes: List[TableNode]): List[TableNode] = { 
    val merged = lowestVariable(nodes) match {
      case Some(v) => mergeLowest(groupThenMergeNodes(nodes, v))
      case None => nodes
    }
    merged.map(toTuplesNode(_))
  }
  
  /* The cost of a Table node is... */
  def cost(n: TableNode) = n match {
    // ...the number of rows that satisfy it, if it's a QueryNode
    case q: QueryNode => client.count(q.conj.partialQuery)
    // ...or the number of rows, if it's a TuplesNode
    case t: TuplesNode => t.tuples.size
  }

  /* Joining two tuples nodes together on a variable is easy, since all of
   * the necessary data is in memory. All that needs to be done is to 
   * create the correct join predicate, and then execute the NestedLoopJoin.
   */
  def joinTT(tn1: TuplesNode, tn2: TuplesNode, v: TVariable): Tuples = {
    val attrPairs = for(a1 <- tn1.getJoinAttrs(v); a2 <- tn2.getJoinAttrs(v))
      yield (a1, a2)
    val pred = Joiner.pairsToCond(attrPairs)
    NestedLoopJoin(pred)(tn1.tuples, tn2.tuples)
  }
  
  /* Joining a QueryNode with a TuplesNode involves doing a partial search
   * join, which executes a query for each row in the TuplesNode.
   */
  def joinQT(qn: QueryNode, tn: TuplesNode, v: TVariable): Tuples = {
    
    // Get the names of the attributes to join on.
    val attrPairs = for (
        a1 <- tn.getJoinAttrs(v);
        a2 <- qn.getJoinAttrs(v)) yield (a1, a2)
        
    // The join predicate uses string similarity.
    val bpred = attrPairs match {
      case (a1, a2) :: tail => AttrsSim(a1, a2, 0.9) 
      case _ => AttrsSim("", "", 0.0)
    }
    
    // If there are multiple join conditions between the QueryNode and 
    // TuplesNode (i.e. if they share more than one variable) then the 
    // additional constraints need to be encoded in a Select predicate, since
    // a partial search join can only involve a single field.
    val spred = attrPairs match {
      case (a1, a2) :: tail => Joiner.pairsToCond(tail)
      case _ => Joiner.truep
    }
    val left = tn.tuples
    val right = PartialSearchFor(qn.conj.name, maxJoinHits, qn.conj.partialQuery)
    val joined = PartialSearchJoin(bpred)(left, right)
    Select(spred)(joined)
  }

  /* Finds the nodes that have the given variable. Merges them together, leaving
   * the other nodes unmerged.
   */
  def groupThenMergeNodes(nodes: List[TableNode], v: TVariable): List[TableNode] = {
    val (toMerge, toKeep) = nodes.partition(_.hasVariable(v))
    mergeNodes(toMerge, v) +: toKeep
  }
  
  /* Finds the lowest-cost variable to merge. In order to be considered for 
   * merging, a variable must occur in at least two nodes. Variables are then
   * assigned a cost equal to the lowest-costing node that they occur in. 
   */
  def lowestVariable(nodes: List[TableNode]): Option[TVariable] = {
    val lst = for (n <- nodes; v <- n.joinAttrs.keySet) yield (v, n)
    val varNodes = lst.groupBy(e => e._1).mapValues(e => e.map(x => x._2).toSet)
    val varCosts = { for ((v, nodes) <- varNodes;
         if nodes.size > 1;
         costs = nodes.map(cost(_));
         minCost = costs.min) yield (v, minCost) }.toMap
    if (varCosts.size > 0) {
      val vars = varCosts.keys
      val lowest = vars.minBy(varCosts(_))
      logger.debug(s"Lowest variable chosen: $lowest")
      Some(lowest)
    } else {
      None
    }
  } 
  
  /* Merges the given nodes together, and removes the variable. */
  def mergeNodes(nodes: List[TableNode], v: TVariable): TuplesNode = {
    val sorted = nodes.sortBy(cost)
    val node = eliminateVar(sorted, v)
    TuplesNode(node.tuples, node.joinAttrs-v)
  }

  /* Recursively eliminates the variable from the given list of nodes. 
   */
  def eliminateVar(nodes: List[TableNode], v: TVariable): TuplesNode = {
    nodes match {
      case node :: Nil => toTuplesNode(node)
      case node1 :: node2 :: rest => eliminateVar(doJoin(node1, node2, v) :: rest, v)
      case _ => throw new IllegalArgumentException("empty node list")
    }
  }
  
  /* Joins together two nodes on the given variable. */
  def doJoin(n1: TableNode, n2: TableNode, v: TVariable): TuplesNode = {
    val t = toTuplesNode(n1)
    val tuples = n2 match {
      case q: QueryNode => joinQT(q, t, v)
      case t2: TuplesNode => joinTT(t, t2, v)
    }
    val merged = Joiner.mergeJoinAttrs(n1.joinAttrs, n2.joinAttrs)
    TuplesNode(tuples.toList, merged)
  }
  
  /* Converts a TableNode to a TuplesNode. If the given node is already a 
   * TuplesNode, does nothing. If it is a QueryNode, it executes it and
   * wraps the resulting tuples as a TuplesNode.
   */
  def toTuplesNode(node: TableNode): TuplesNode = node match {
    case t: TuplesNode => t
    case q: QueryNode => queryToTuples(q)
    case _ => throw new IllegalArgumentException("invalid node type: " + node)
  }
  
  /* Executes the given QueryNode to create a TuplesNode. */
  def queryToTuples(q: QueryNode): TuplesNode = {
    logger.debug(s"Making TuplesNode from $q")
    val tuples = SearchFor(q.conj.name, q.conj.partialQuery)
    val result = TuplesNode(tuples, q.joinAttrs)
    logger.debug(s"Done making TuplesNode from $q")
    result
  }

}

/* Companion object for Joiner. */
case object Joiner {
  
  // Mnemonic
  type JA = Map[TVariable, List[String]]
  
  // The join condition defaults to thresholded string similarity.
  val eqCond = (a1: String, a2: String) => AttrsSim(a1, a2, 0.9)
  
  // Merges the given maps.
  def mergeJoinAttrs(attrs1: JA, attrs2: JA): JA = {
    val allVars = attrs1.keySet union attrs2.keySet
    val newPairs = for (v <- allVars;
         as1 = attrs1.getOrElse(v, Nil).toSet;
         as2 = attrs2.getOrElse(v, Nil).toSet)
      yield (v, (as1 ++ as2).toList)
    return newPairs.toMap
  }
  
  // Tuple predicate that always returns true.
  val truep = (t: Tuple) => true
  
  // The conjunction of two tuple predicates.
  def and(p1: TuplePred, p2: TuplePred) = (t: Tuple) => p1(t) && p2(t)
  
  // The conjunction of a list of predicates.
  def andList(preds: Iterable[TuplePred]): TuplePred = 
    preds.foldLeft(truep)(and)
    
  // Takes a list of tuple attribute pairs, returns a single predicate
  // encoding them as a conjunction of join conditions.
  def pairsToCond(pairs: List[(String, String)]): TuplePred = {
    val preds = for ((a1, a2) <- pairs) yield eqCond(a1, a2)
    andList(preds)
  }
  
}

/* TuplesNodes can be either TableNodes or QueryNodes. */
case class TuplesNode(tuples: List[Tuple], 
    joinAttrs: Map[TVariable, List[String]]) extends TableNode {
}

/* TableNodes store data from partially-executed queries. */
trait TableNode {
  val joinAttrs: Map[TVariable, List[String]]
  def getJoinAttrs(v: TVariable) = joinAttrs.getOrElse(v, Nil)
  def hasVariable(v: TVariable) = joinAttrs.contains(v)
}

/* QueryNodes represent unexecuted queries. */
case class QueryNode(conj: TConjunct) extends TableNode {
  val jks = conj.joinKeys
  val joinAttrs = { 
    for (v <- jks.keys; attr <- jks.get(v)) yield (v, List(attr)) 
  }.toMap
}
