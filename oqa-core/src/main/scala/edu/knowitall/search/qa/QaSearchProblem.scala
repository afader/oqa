package edu.knowitall.search.qa

import edu.knowitall.paraphrasing.template.TemplateParaphraser
import edu.knowitall.execution.IdentityExecutor
import edu.knowitall.triplestore.SolrClient
import edu.knowitall.search.SearchProblem
import edu.knowitall.search.BeamSearch
import edu.knowitall.search.Transition
import edu.knowitall.tool.postag.StanfordPostagger
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.triplestore.CachedTriplestoreClient

case class QaSearchProblem(
    question: String,
    transitionModel: Transition[QaState, QaAction] = 
      QaSearchProblem.transitionModel,
    costModel: Function[QaStep, Double] = QaSearchProblem.costModel) 
    extends SearchProblem[QaState, QaAction] {

  val initialState = QuestionState(question)
  
  override def successors(s: QaState) = transitionModel(s)
    
  override def isGoal(s: QaState) = s match {
    case as: AnswerState => true
    case _ => false
  }
  
  override def cost(fromState: QaState, action: QaAction, toState: QaState) =
    costModel(QaStep(question, fromState, action, toState))

}

object QaSearchProblem {
  
  val transitionModel = new QaTransitionModel
  
  val costModel = new QaCostModel
  
}
