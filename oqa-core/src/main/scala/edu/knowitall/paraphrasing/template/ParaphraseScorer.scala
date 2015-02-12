package edu.knowitall.paraphrasing.template

import edu.knowitall.paraphrasing.ScoredParaphraseDerivation

trait ParaphraseScorer {
  
  def scoreAll(derivs: Iterable[TemplateParaphraseDerivation]): Iterable[TemplateParaphraseDerivation]

}