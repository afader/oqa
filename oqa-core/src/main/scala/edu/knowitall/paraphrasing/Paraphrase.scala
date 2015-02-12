package edu.knowitall.paraphrasing

abstract class Paraphrase private () {
  def source: String
  def target: String
  def derivation: ScoredParaphraseDerivation
}

object Paraphrase {
  private case class ParaphraseImpl(source: String, target: String, 
      derivation: ScoredParaphraseDerivation) extends Paraphrase
  def apply(source: String, target: String,
      derivation: ScoredParaphraseDerivation): Paraphrase = 
        ParaphraseImpl(source, target, derivation)
}