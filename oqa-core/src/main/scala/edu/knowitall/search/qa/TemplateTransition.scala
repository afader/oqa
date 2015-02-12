package edu.knowitall.search.qa

import edu.knowitall.search.Transition
import edu.knowitall.paraphrasing.template.ParaphraseTemplateClient
import com.typesafe.config.ConfigFactory
import edu.knowitall.paraphrasing.template.TemplatePair
import org.apache.solr.client.solrj.SolrServerException
import java.io.StringWriter
import java.io.PrintWriter
import org.slf4j.LoggerFactory

class TemplateTransition(client: ParaphraseTemplateClient, skipTimeouts: Boolean = TemplateTransition.defaultSkipTimeouts) extends Transition[QaState, QaAction] {
  
  def this() = this(new ParaphraseTemplateClient())
  
  private val logger = LoggerFactory.getLogger(this.getClass) 
  
  override def apply(s: QaState) = s match {
    case s: AbstractedArgState => paraphrase(s)
    case _ => Nil
  }
  
  private def paraphrases(s: String, argTypes: List[String]) = try {
    client.paraphrases(s, argTypes)
  } catch {
    case e: Throwable => if (skipTimeouts) {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      e.printStackTrace(pw)
      logger.warn(s"Could not get paraphrases: $s, got ${sw.toString}")
      List.empty
    } else {
      throw e
    }
  }
  
  private def paraphrase(state: AbstractedArgState) = 
    for {
      templatePair <- paraphrases(state.queryString, state.argTypes)
      arg = state.arg
      newQuestion = applyTemplate(arg, templatePair)
      action = templatePair
      newState = QuestionState(newQuestion, true)
    } yield (action, newState)
  
  private def applyTemplate(arg: String, pair: TemplatePair) = 
    pair.template2.replace("$y", arg)

}

object TemplateTransition {
  val conf = ConfigFactory.load()
  val defaultSkipTimeouts = conf.getBoolean("paraphrase.template.skipTimeouts")
}