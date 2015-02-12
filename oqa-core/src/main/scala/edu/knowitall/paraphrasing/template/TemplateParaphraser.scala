package edu.knowitall.paraphrasing.template

import edu.knowitall.paraphrasing.Paraphraser
import edu.knowitall.tool.postag.StanfordPostagger
import edu.knowitall.tool.tokenize.ClearTokenizer
import org.slf4j.LoggerFactory
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.paraphrasing.Paraphrase
import com.typesafe.config.ConfigFactory

case class TemplateParaphraser(scorer: ParaphraseScorer, generator: TemplateParaphraseGenerator) extends Paraphraser {
  
  def this() = this(TemplateParaphraser.defaultScorer, new SolrParaphraseGenerator())
  
  lazy val tagger = new StanfordPostagger()
  lazy val tokenizer = new ClearTokenizer()
  val logger = LoggerFactory.getLogger(this.getClass)
  
  def stemString(s: String): Seq[String] = {
    val tokens = tokenizer(s)
    val tagged = tagger.postagTokenized(tokens)
    val lemmas = tagged.map(t => MorphaStemmer.lemmatizePostaggedToken(t).lemma.toLowerCase()) 
    lemmas
  }
  
  override def paraphrase(s: String) = {
    val stemmed = stemString(s)
    val paraphrases = generator.generate(stemmed)
    val scored = scorer.scoreAll(paraphrases).toList
    val grouped = scored.groupBy(sp => sp.paraphrase.question).values
    val maxed = grouped.map(g => g.maxBy(d => d.score)).toList
    val sorted = maxed.sortBy(d => -d.score)
    for (deriv <- sorted; target = deriv.paraphrase.question.mkString(" "))
      yield Paraphrase(s, target, deriv)
  }
}

case object TemplateParaphraser {
  
  val conf = ConfigFactory.load()
  val scoringModel = conf.getString("paraphrase.template.scoringModel")
  val defaultScorer = scoringModel match {
    case "pmi" => new PmiParaphraseScorer()
    case "lm" => new LmParaphraseScorer()
    case "pmiLm" => new PmiLmParaphraseScorer()
    case _ => throw new IllegalStateException(s"Could not load default scoring model '$scoringModel'")
  }

  
}