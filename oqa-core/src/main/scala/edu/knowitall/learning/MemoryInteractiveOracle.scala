package edu.knowitall.learning

import edu.knowitall.eval.FileOracle
import org.slf4j.LoggerFactory
import edu.knowitall.model.Derivation

class MemoryInteractiveOracle(oracle: FileOracle) extends CorrectnessModel[String, Derivation] {
  
  val logger = LoggerFactory.getLogger(this.getClass)
  
  def this(path: String) = this(new FileOracle(path))
  
  val labeled = new LabeledDataOracle(oracle)
  val interactive = new InteractiveOracle()
  
  override def isCorrect(question: String, deriv: Derivation) = {
    val answer = deriv.answer
    if (oracle.hasLabel(question, answer)) {
      logger.debug(s"Using saved labels for $question")
      labeled.isCorrect(question, deriv)
    } else {
      val result = interactive.isCorrect(question, deriv)
      oracle.update(question, answer, result)
      oracle.save
      result
    }
  }
  
  private def haveLabelFor(question: String, deriv: Derivation) = 
    oracle.hasLabel(question, deriv.answer)
  
  override def pickCorrect(question: String, derivs: Seq[Derivation]) = {
    labeled.pickCorrect(question, derivs) match {
      case Some(deriv) => {
        logger.debug(s"Using saved labels for $question")
        logger.debug(s"Found '${deriv.answer}' as answer for '$question'")
        Some(deriv)
      }
      case None => {
        val unlabeled = derivs.filter(d => !haveLabelFor(question, d))
        interactive.pickCorrectMultiple(question, unlabeled) match {
          case seq: Seq[Derivation] if seq.size > 0 => {
            for (d <- seq) {
              oracle.update(question, d.answer, true)
            }
            oracle.save
            logger.debug(s"Found '${seq(0)}' as answer for '$question'")
            Some(seq(0))
          }
          case Nil => {
            for (d <- unlabeled) {
              oracle.update(question, d.answer, false)
            }
            oracle.save
            logger.debug(s"Correct answer for '$question' is unreachable")
            None
          }
        }
        
      }
    }
  }

}