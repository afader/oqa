package edu.knowitall.search.qa

import java.util.Comparator

object QaStateComparator extends Comparator[QaState] {
  def statePosition(s: QaState) = s match {
    case _: AnswerState => 0
    case _: TupleState => 1
    case _: QueryState => 2
    case _: AbstractedArgState => 3
    case _: QuestionState => 3
    case _ => 
      throw new IllegalStateException(s"Could not find ordering for state $s")
  }
  override def compare(s1: QaState, s2: QaState) = 
    statePosition(s1) compareTo statePosition(s2) 
}