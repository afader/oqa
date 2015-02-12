package edu.knowitall.search.qa

import edu.knowitall.search.Transition

class ProjectionTransition extends Transition[QaState, QaAction] {
  override def apply(s: QaState) = s match {
    case ts: TupleState => project(ts)
    case _ => Nil
  }
  def project(ts: TupleState) = 
    List((ProjectAction, AnswerState(ts.execTuple.answerString)))
}

object ProjectAction extends QaAction