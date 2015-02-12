package edu.knowitall.search.qa

case class QaStep(question: String, fromState: QaState, action: QaAction,
    toState: QaState)