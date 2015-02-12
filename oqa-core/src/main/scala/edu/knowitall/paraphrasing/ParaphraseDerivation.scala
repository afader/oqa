package edu.knowitall.paraphrasing

import com.typesafe.config.ConfigFactory

trait ParaphraseDerivation

trait ScoredParaphraseDerivation extends ParaphraseDerivation {
  def score: Double
}

case object IdentityDerivation extends ScoredParaphraseDerivation {
  val conf = ConfigFactory.load()
  override val score = conf.getDouble("paraphrase.identityScore")
}