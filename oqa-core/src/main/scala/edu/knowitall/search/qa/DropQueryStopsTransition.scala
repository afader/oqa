package edu.knowitall.search.qa

import edu.knowitall.search.Transition
import org.slf4j.LoggerFactory
import edu.knowitall.execution.TLiteral
import edu.knowitall.execution.UnquotedTLiteral
import edu.knowitall.util.NlpTools
import edu.knowitall.execution.ListConjunctiveQuery
import edu.knowitall.execution.TConjunct
import edu.knowitall.execution.ConjunctiveQuery

object DropStopAction extends QaAction {
  override def toString = "DropStopAction"
}

class DropQueryStopsTransition(
    val stops: Set[String] = DropQueryStopsTransition.defaultStops) 
    extends Transition[QaState, QaAction] {
  
  val logger = LoggerFactory.getLogger(this.getClass)

  override def apply(s: QaState) = s match {
    case qs: QueryState if (!qs.reformulated) => dropStops(qs.query)
    case _ => Nil
  }
  
  def dropStops(query: ConjunctiveQuery) = {
    val newConjs = query.conjuncts.map(dropStopsConj)
    val newQuery = new ListConjunctiveQuery(query.qVars, newConjs)
    List((DropStopAction, QueryState(newQuery)))
  }
  
  private def dropStopsConj(conj: TConjunct) = {
    val newVals = for ((f, v) <- conj.values) yield v match {
      case UnquotedTLiteral(s) => (f, UnquotedTLiteral(dropStopsString(s)))
      case _ => (f, v)
    }
    new TConjunct(conj.name, newVals)
  }
  
  private def dropStopsString(s: String) = {
    val sent = NlpTools.process(s)
    val lemmas = sent.lemmatizedTokens.map(_.lemma)
    if (lemmas.forall(stops contains _)) {
      s
    } else {
      val filtered = lemmas.filterNot(stops contains _)
      filtered.mkString(" ")
    }
  }

}

case object DropQueryStopsTransition {
  val defaultStops = Set("the", "a", "an", "be", "have", "do", "'s", "can", "will", "get")
}