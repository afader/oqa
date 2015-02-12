package edu.knowitall.search.qa

import edu.knowitall.execution.ExecTuple

case class AnswerState(answer: String) extends QaState {
  override def stateType = "AnswerState"
  override def toString = answer
}