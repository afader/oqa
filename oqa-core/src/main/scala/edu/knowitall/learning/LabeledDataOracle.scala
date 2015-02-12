package edu.knowitall.learning

import edu.knowitall.eval.Oracle
import edu.knowitall.eval.FileOracle
import edu.knowitall.model.Derivation

class LabeledDataOracle(oracle: Oracle) extends CorrectnessModel[String, Derivation] {
  
  def this(path: String) = this(new FileOracle(path))
  
  def isCorrectAnswer(question: String, answer: String) =
    oracle.getLabel(question, answer).getOrElse(false)
    
  override def isCorrect(question: String, deriv: Derivation) =
    isCorrectAnswer(question, deriv.answer)
  
  override def pickCorrect(question: String, derivs: Seq[Derivation]) =
    derivs.filter(answer => isCorrect(question, answer)) match {
      case d :: ds => Some(d)
      case Nil => None
    }

}