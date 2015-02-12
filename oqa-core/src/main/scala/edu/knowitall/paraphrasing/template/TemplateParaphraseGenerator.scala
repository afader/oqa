package edu.knowitall.paraphrasing.template
import edu.knowitall.collection.immutable.Interval
import com.typesafe.config.ConfigFactory
import edu.knowitall.paraphrasing.ScoredParaphraseDerivation
import edu.knowitall.lm.KenLmServer

case class ArgQuestion(question: Seq[String], argInterval: Interval) {
  def arg: String = question.slice(argInterval.start, argInterval.end).mkString(" ")
}

case class TemplateParaphraseDerivation(question: ArgQuestion, 
    paraphrase: ArgQuestion, templates: TemplatePair, score: Double = 0.0, pmi: Double = 0.0, lm: Double = 0.0) extends ScoredParaphraseDerivation {
  val questionString = question.question.mkString(" ")
}

trait TemplateParaphraseGenerator {
  def generate(question: Seq[String]): Iterable[TemplateParaphraseDerivation]
}

class SolrParaphraseGenerator(url: String, maxHits: Int, maxArgLength: Int) extends TemplateParaphraseGenerator {
  def this() = this(SolrParaphraseGenerator.defaultUrl, 
      SolrParaphraseGenerator.defaultMaxHits,
      SolrParaphraseGenerator.defaultMaxArgLength)
  val client = new ParaphraseTemplateClient()
  def intervals(size: Int) =
    for (i <- Range(0, size); j <- Range(i, size); if j+1-i <= maxArgLength) yield Interval.open(i, j+1)
  
  def templates(q: ArgQuestion): List[TemplatePair] = {
    val i = q.argInterval.start
    val j = q.argInterval.end
    val n = q.question.size
    val left = q.question.slice(0, i).mkString(" ")
    val right = q.question.slice(j, n).mkString(" ")
    val query = left + " $y " + right
    client.paraphrases(query, limit = maxHits)
  }
  
  def abstractQuestion(q: Seq[String]): Iterable[ArgQuestion] = {
    val n = q.size
    for (i <- intervals(n)) yield ArgQuestion(q, i)
  }
  
  def substitute(q: ArgQuestion, t: TemplatePair): ArgQuestion = {
    val templ = t.template2
    val templSeq = templ.split(" ").toSeq
    val arg = q.question.slice(q.argInterval.start, q.argInterval.end)
    val i = templSeq.indexOf("$y")
    if (i >= 0) {
      val left = templSeq.slice(0, i)
      val right = templSeq.slice(i+1, templSeq.size)
      val para = ArgQuestion(left ++ arg ++ right, Interval.open(i, i+arg.size))
      para
    } else {
      throw new IllegalArgumentException(s"Could not find var in: $templ")
    }
    
  }
  
  override def generate(question: Seq[String]): Iterable[TemplateParaphraseDerivation] = {
    for (aq <- abstractQuestion(question); t <- templates(aq); para = substitute(aq, t)) 
      yield TemplateParaphraseDerivation(aq, para, t, score = 0.0, pmi = t.pmi, lm = Double.MinValue)
      
  }

}

case object SolrParaphraseGenerator {
  val conf = ConfigFactory.load()
  val defaultUrl = conf.getString("paraphrase.template.url")
  val defaultMaxHits = conf.getInt("paraphrase.template.maxHits")
  val defaultMaxArgLength = conf.getInt("paraphrase.template.maxArgLength")
}