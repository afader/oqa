package edu.knowitall.search.qa

import edu.knowitall.parsing.cg.CgParser
import edu.knowitall.search.Transition

case class CgParseTransition(parser: CgParser = CgParseTransition.defaultParser) extends Transition[QaState, QaAction] {
  
  override def apply(s: QaState) = s match {
    case s: QuestionState => parseQuestion(s)
    case _ => Nil
  }
  
  def parseQuestion(s: QuestionState) = {
    val question = s.question
    for {
      derivation <- parser.parse(question)
      newState = QueryState(derivation.query)
    } yield (derivation, newState)
    
  }

}

case object CgParseTransition {
  lazy val defaultParser = CgParser()
}