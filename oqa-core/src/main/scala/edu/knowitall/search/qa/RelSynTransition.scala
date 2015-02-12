package edu.knowitall.search.qa

import edu.knowitall.relsyn.SolrRelSynClient
import edu.knowitall.search.Transition
import edu.knowitall.execution.TConjunct
import edu.knowitall.execution.ListConjunctiveQuery
import org.slf4j.LoggerFactory
import edu.knowitall.execution.Search
import com.typesafe.config.ConfigFactory
import org.apache.solr.client.solrj.SolrServerException
import java.io.StringWriter
import java.io.PrintWriter
import edu.knowitall.relsyn.RelSynClient

class RelSynTransition(client: RelSynClient = RelSynTransition.defaultClient, skipTimeouts: Boolean = RelSynTransition.defaultSkipTimeouts, multipleSyns: Boolean = RelSynTransition.defaultMultipleSyns) 
  extends Transition[QaState, QaAction] {
  
  val logger = LoggerFactory.getLogger(this.getClass)

  override def apply(s: QaState) = s match {
    case qs: QueryState if (!qs.reformulated || multipleSyns) => reformulate(qs)
    case _ => Nil
  }
  
  private def reformulate(s: QueryState) = {
    val conjs = s.query.conjuncts
    for {
      i <- 0 until conjs.size
      c = conjs(i)
      rule <- relSyns(c)
      newc <- rule(c)
      newconjs = conjs.updated(i, newc)
      newq = ListConjunctiveQuery(s.query.qVars, newconjs)
      newstate = s.copy(query = newq, reformulated = true)
    } yield {
      (rule, newstate)
    }
  }
  
  private def relSyns(c: TConjunct) = try {
    client.relSyns(c)
  } catch {
    case e: SolrServerException => if (skipTimeouts) {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      e.printStackTrace(pw)
      logger.warn(s"Could not reformulate query: $c, got ${sw.toString}")
      List.empty
    } else {
      throw e
    }
  }

}

object RelSynTransition {
  val conf = ConfigFactory.load()
  val defaultSkipTimeouts = conf.getBoolean("relsyn.skipTimeouts")
  val defaultMultipleSyns = conf.getBoolean("relsyn.multipleSyns")
  lazy val defaultClient = new SolrRelSynClient() 
}