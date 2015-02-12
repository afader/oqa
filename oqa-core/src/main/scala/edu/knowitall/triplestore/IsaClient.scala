package edu.knowitall.triplestore

import edu.knowitall.relsyn.IsaRelSynClient
import edu.knowitall.execution.ListConjunctiveQuery
import edu.knowitall.execution.IdentityExecutor
import edu.knowitall.util.NlpTools
import edu.knowitall.execution.Search.TSQuery
import edu.knowitall.model.QaModel

case class IsaClient(client: TriplestoreClient = IsaClient.defaultTriplestoreClient, maxHits: Int = 500) {
  
  private val internalClient = new TriplestoreClient {
    override def search(q: TSQuery, h: Int) = client.search(q, maxHits)
    override def count(q: TSQuery) = client.count(q)
  }
  
  private val isaSyns = IsaRelSynClient
  private val exec = IdentityExecutor(internalClient)
  private val norm = (s: String) => NlpTools.normalize(s)
  private def getQuery(a: String) = ListConjunctiveQuery.fromString("$x : (\"" + a.replace(",", " ") + "\", isa, $x)") match {
    case Some(q) => q
    case None => throw new IllegalStateException(s"Could not make query from $a")
  }
  private def getQueries(a: String) = {
    val q = getQuery(a)
    val c = q.conjuncts(0)
    val rules = isaSyns.relSyns(c)
    val conjs = rules.flatMap(_(c))
    conjs map { c => ListConjunctiveQuery(q.qVars, List(c)) }
  }
  def getTypes(a: String) = {
    val queries = getQueries(a)
    val results = queries flatMap { q => exec.execute(q) }
    "anything" :: results.map(a => norm(a.answerString)).distinct
  }
}

case object IsaClient {
  lazy val defaultTriplestoreClient = new SolrClient()
}