package edu.knowitall.search.qa

import edu.knowitall.execution.ExecTuple

case class TupleState(execTuple: ExecTuple) extends QaState {
  override def stateType = "TupleState"
  override def toString = execTuple.toTripleString
}