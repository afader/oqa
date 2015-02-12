package edu.knowitall.paraphrasing.template
import scala.io.Source
import scala.collection.JavaConversions._
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer
import org.apache.solr.client.solrj.SolrServer
import org.slf4j.LoggerFactory
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.SortClause
import scala.Option.option2Iterable
import com.typesafe.config.ConfigFactory
import edu.knowitall.util.MathUtils
import edu.knowitall.search.qa.QaAction
import edu.knowitall.triplestore.SolrClient
import edu.knowitall.util.ResourceUtils
import org.apache.solr.common.params.GroupParams
import org.apache.solr.client.solrj.response.QueryResponse

case class ParaphraseTemplateClient(solrUrl: String,
    maxHits: Int, scale: Boolean = ParaphraseTemplateClient.scale, 
    timeout: Int = ParaphraseTemplateClient.defaultTimeout,
    stopTemplates: Set[String] = ParaphraseTemplateClient.stopTemplates) {
  
  def this() = this(ParaphraseTemplateClient.defaultUrl, ParaphraseTemplateClient.defaultMaxHits, ParaphraseTemplateClient.scale)
  
  val logger = LoggerFactory.getLogger(this.getClass)
  val server = new HttpSolrServer(solrUrl)
  server.setConnectionTimeout(timeout)
  server.setSoTimeout(timeout)
  server.setMaxRetries(1)
  val searchField = "template1_exact"
  
  def paraphrases(s: String, argTypes: List[String] = List("anything"), limit: Int = maxHits) = 
    queryParaphrases(s, argTypes, limit)
  
  private def responseToPairs(r: QueryResponse) = for {
    value <- r.getGroupResponse.getValues
    groupValue <- value.getValues
    doc <- groupValue.getResult.toList
    pair <- TemplatePair.fromDocument(doc)
    if !(stopTemplates contains pair.template2)
  } yield pair
  
  private def createQuery(s: String, argTypes: List[String], limit: Int = maxHits) = {
    val typePred = { argTypes map { t =>
      val esc = SolrClient.escape(t)
      s"""typ_exact:"${t}""""
    } }.mkString(s" OR ")
    val qStr = s"""${searchField}:"${s}" AND ($typePred)"""
    val query = new SolrQuery(SolrClient.fixQuery(qStr))
    query.setRows(maxHits)
    query.addSort(new SortClause("pmi", SolrQuery.ORDER.desc))
    query.set(GroupParams.GROUP, true)
    query.set(GroupParams.GROUP_FIELD, "template2_exact")
    query.setParam("shards.tolerant", true)
    query
  }
    
  def queryParaphrases(s: String, argTypes: List[String] = List("anything"), limit: Int = maxHits): List[TemplatePair] = {
    val query = createQuery(s, argTypes, limit)
    val resp = server.query(query)
    val pairs = responseToPairs(resp).toList
    pairs
  } 
}

case object ParaphraseTemplateClient {
  val conf = ConfigFactory.load()
  val minPmi = conf.getDouble("paraphrase.template.minPmi")
  val maxPmi = conf.getDouble("paraphrase.template.maxPmi")
  val scale = conf.getBoolean("paraphrase.template.scale")
  val defaultUrl = conf.getString("paraphrase.template.url")
  val defaultMaxHits = conf.getInt("paraphrase.template.maxHits")
  val defaultTimeout = conf.getInt("paraphrase.template.timeout")
  val stopTemplatesPath = conf.getString("paraphrase.template.stopTemplatesPath")
  lazy val stopTemplates = ResourceUtils.resourceSource(stopTemplatesPath).getLines.toSet
}

case class TemplatePair(template1: String, template2: String, typ: String, count1: Double, count2: Double, count12: Double, pmi: Double) extends QaAction

case object TemplatePair {
    
  def fromString(s: String): Option[TemplatePair] = {
    s.split("\t", 9) match {
      case Array(t1, t2, typ, count1, count2, count12, pmi) =>
         Some(TemplatePair(t1, t2, typ, count1.toDouble, count2.toDouble, count12.toDouble, pmi.toDouble))
      case _ => None
    }
  }
  def fromDocument(doc: SolrDocument): Option[TemplatePair] = {
    val t1obj: Any = doc.getFieldValue("template1")
    val t2obj: Any = doc.getFieldValue("template2")
    val typobj: Any = doc.getFieldValue("typ")
    val count1obj: Any = doc.getFieldValue("count1")
    val count2obj: Any = doc.getFieldValue("count2")
    val count12Obj: Any = doc.getFieldValue("count12")
    val pmiObj: Any = doc.getFieldValue("pmi")
    (t1obj, t2obj, typobj, count1obj, count2obj, count12Obj, pmiObj) match {
      case (t1: String, t2: String, typ: String, count1: Float, count2: Float, count12: Float, pmi: Float) => Some(TemplatePair(t1, t2, typ, count1, count2, count12, pmi))
      case _ => None
    }
  }
}
