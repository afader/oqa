package edu.knowitall.search.qa

import edu.knowitall.paraphrasing.rules.ParaphraseRuleSet
import edu.knowitall.search.Transition

case class ParaphraseRuleTransition(ruleSet: ParaphraseRuleSet = ParaphraseRuleSet()) extends Transition[QaState, QaAction] {
  
  override def apply(s: QaState) = s match {
    case s: QuestionState => paraphrase(s)
    case _ => Nil
  }
  
  private def paraphrase(qs: QuestionState) = for {
    (rule, result) <- ruleSet.apply(qs.processed)
    newState = QuestionState(result, qs.isParaphrased)
  } yield (rule, newState)

}