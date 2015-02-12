package edu.knowitall.search.qa

import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.util.NlpTools

trait QuestionState extends QaState {
  def question: String
  def processed: Sentence with Lemmatized with Chunked
  def isParaphrased: Boolean
  override def stateType = "QuestionState"
  override def toString = question
}

case object QuestionState {
  private case class QuestionStateImpl(
      question: String,
      processed: Sentence with Lemmatized with Chunked,
      isParaphrased: Boolean = false) extends QuestionState
  def apply(question: String): QuestionState = QuestionStateImpl(question, NlpTools.process(question))
  def apply(q: String, isP: Boolean): QuestionState = QuestionStateImpl(q, NlpTools.process(q), isP)
}