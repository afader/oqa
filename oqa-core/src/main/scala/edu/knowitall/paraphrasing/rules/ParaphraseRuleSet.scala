package edu.knowitall.paraphrasing.rules

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.util.ResourceUtils
import com.typesafe.config.ConfigFactory
import edu.knowitall.util.NlpTools
import scala.io.Source

case class ParaphraseRuleSet(rules: List[ParaphraseRule] = ParaphraseRuleSet.defaultRules) {
  def apply(s: Sentence with Chunked with Lemmatized) = for {
    r <- rules
    p <- r(s)
  } yield (r, p)
}

case object ParaphraseRuleSet {
  val conf = ConfigFactory.load()
  val defaultRuleSetPath = conf.getString("paraphrase.rules.ruleSetPath")
  lazy val defaultRules = {
    val strings = ResourceUtils.resourceSource(defaultRuleSetPath).getLines.toIndexedSeq
    ParaphraseRule.fromStrings(strings)
  }.toList
  def fromPath(p: String) = {
    val lines = Source.fromFile(p, "UTF-8").getLines.toIndexedSeq
    ParaphraseRuleSet(ParaphraseRule.fromStrings(lines).toList)
  }
}