package edu.knowitall.search.qa

import edu.knowitall.execution.ConjunctiveQuery

case class QueryState(query: ConjunctiveQuery, reformulated: Boolean = false) 
extends QaState {
  override def stateType = "QueryState"
  override def toString = query.toString
}