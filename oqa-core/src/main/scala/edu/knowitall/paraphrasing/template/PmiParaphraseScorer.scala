package edu.knowitall.paraphrasing.template

import edu.knowitall.lm.KenLmServer

class PmiParaphraseScorer() extends ParaphraseScorer {
  val client = new KenLmServer()
  override def scoreAll(derivs: Iterable[TemplateParaphraseDerivation]): Iterable[TemplateParaphraseDerivation] = {
    val dlist = derivs.toList
    val lmScores = client.query(dlist.map(_.paraphrase.question.mkString(" "))).map(_._2)
    for ((d, score) <- dlist.zip(lmScores)) yield d.copy(score = d.templates.pmi) 
  }
}