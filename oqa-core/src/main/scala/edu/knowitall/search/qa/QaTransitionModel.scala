package edu.knowitall.search.qa

import edu.knowitall.tool.postag.StanfordPostagger
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.paraphrasing.template.ParaphraseTemplateClient
import edu.knowitall.triplestore.SolrClient
import edu.knowitall.triplestore.CachedTriplestoreClient
import edu.knowitall.search.Transition
import edu.knowitall.relsyn.SolrRelSynClient
import edu.knowitall.relsyn.IsaRelSynClient
import edu.knowitall.util.NlpTools
import com.typesafe.config.ConfigFactory
import edu.knowitall.parsing.cg.CgParser

class QaTransitionModel extends Transition[QaState, QaAction] {
  
  lazy val cgParser = new CgParser()
  
  // Remote services
  lazy val templateClient = new ParaphraseTemplateClient
  lazy val baseTriplestoreClient = new SolrClient()
  lazy val triplestoreClient = CachedTriplestoreClient(baseTriplestoreClient)
  lazy val relSynClient = SolrRelSynClient()
  
  // Individual transition functions
  lazy val absArgTransition = new AbstractArgTransition()
  lazy val templateTransition = new TemplateTransition(templateClient)
  lazy val paraRuleTransition = ParaphraseRuleTransition()
  lazy val parseTransition = new CgParseTransition(cgParser)
  lazy val executeTransition = new ExecutionTransition(triplestoreClient)
  lazy val isaSynTransition = new RelSynTransition(IsaRelSynClient)
  lazy val relSynTransition = new RelSynTransition(relSynClient)
  lazy val dropStopsTransition = new DropQueryStopsTransition()
  lazy val projTransition = new ProjectionTransition
  
  val conf = ConfigFactory.load()
  
  lazy val components = Map(
    "parse" -> parseTransition,
    "templateParaphrase" -> (absArgTransition + templateTransition),
    "ruleParaphrase" -> paraRuleTransition,
    "relSyn" -> relSynTransition,
    "isaRelSyn" -> isaSynTransition,
    "execute" -> executeTransition,
    "project" -> projTransition,
    "dropStops" -> dropStopsTransition
  )
  
  lazy val activeComponents = for {
    name <- components.keys
    active = conf.getBoolean(s"search.transitions.$name")
    if active
    component = components(name)
  } yield component
  
  lazy val model = activeComponents.reduce(_ + _)
  			  
  override def apply(s: QaState) = model(s)

}