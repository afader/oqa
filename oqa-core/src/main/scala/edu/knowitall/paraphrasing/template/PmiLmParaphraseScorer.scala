package edu.knowitall.paraphrasing.template

import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.lm.KenLmServer
import edu.knowitall.paraphrasing.ScoredParaphraseDerivation

class PmiLmParaphraseScorer() extends ParaphraseScorer {
  val client = new KenLmServer()
  override def scoreAll(derivs: Iterable[TemplateParaphraseDerivation]): Iterable[TemplateParaphraseDerivation] = {
    val dlist = derivs.toList
    val lmScores = client.query(dlist.map(_.paraphrase.question.mkString(" "))).map(_._2)
    for ((d, lmScore) <- dlist.zip(lmScores)) yield d.copy(score = -lmScore * d.templates.pmi, lm = lmScore) 
  }
}