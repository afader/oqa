package edu.knowitall.search.qa
import edu.knowitall.paraphrasing.template.TemplatePair
import edu.knowitall.execution.ExecTuple
import edu.knowitall.execution.ConjunctiveQuery
import edu.knowitall.learning.SparseVector
import edu.knowitall.learning.QueryTupleSimilarity
import com.typesafe.config.ConfigFactory
import edu.knowitall.lm.KenLmServer
import edu.knowitall.util.NlpUtils
import edu.knowitall.execution.Search
import edu.knowitall.relsyn.RelSynRule
import com.rockymadden.stringmetric.StringMetric
import edu.knowitall.execution.Tuple
import edu.knowitall.parsing.cg.ParsedQuestion
import edu.knowitall.paraphrasing.rules.ParaphraseRule

object QaFeatures extends Function[QaStep, SparseVector] {
  
  val conf = ConfigFactory.load()
  val defaultPmi = conf.getDouble("paraphrase.defaultPmi")
  val defaultLm = conf.getDouble("paraphrase.defaultLm")
  val lmClient = new KenLmServer()
  
  val tupleTemplates = TupleFeatureTemplate.defaultTemplates
  val tupleFeatures = ExecutionFeature { (question: String, etuple: ExecTuple) =>
    val tuple = etuple.tuple
    tupleTemplates.map(t => t(tuple)).reduce(_ + _)
  }
  
  val answerIsLinked = ExecutionFeature { (question: String, etuple: ExecTuple) =>
    val tuple = etuple.tuple
    val qAttrs = etuple.query.qAttrs
    val isLinked = qAttrs.exists(attr => {
      tuple.get(attr + "_fbid_s") match {
        case Some(value) => true
        case _ => false
      } 
    })
    ("answer is linked to freebase", isLinked)
  }
  
  val actionType = ActionFeature { a: QaAction =>
    (s"action type = ${a.getClass.getSimpleName}", 1.0)
  }
  
  val tupleNamespace = ExecutionFeature { (q: String, etuple: ExecTuple) => 
    val tuple = etuple.tuple
    val nss = tuple.attrs.keys.filter(_.endsWith(".namespace")).flatMap(tuple.getString(_))
    nss.map(ns => s"answer from namespace '$ns'")
  }
  
  val numConjuncts = ExecutionFeature { (q: String, etup: ExecTuple) =>
    ("num conjuncts" -> etup.query.conjuncts.size)
  }
  
  val querySimilarity = ExecutionFeature { (q: String, etuple: ExecTuple) =>
    val query = etuple.query
    val tuple = etuple.tuple
    //val relSim = QueryTupleSimilarity.relSimilarity(query, tuple)
    //val argSim = QueryTupleSimilarity.argSimilarity(query, tuple)
    val quesSim = QueryTupleSimilarity.questionQuerySimilarity(query, q)
    val quesEvSim = QueryTupleSimilarity.questionTupleSimilarity(q, tuple)
    SparseVector("evidence similarity with question" ->quesEvSim,
    			 "query similarity with question" -> quesSim)
  }
  
  def freebaseLink(key: String, tuple: Tuple) = tuple.getString(key + "_fbid_s")
  
  val joinSimilarity = ExecutionFeature { (q: String, etuple: ExecTuple) =>
    val query = etuple.query
    val tuple = etuple.tuple
    val sims = for {
      (key1, key2) <- query.joinPairs
      val1 <- tuple.getString(key1)
      val2 <- tuple.getString(key2)
      s <- StringMetric.compareWithDiceSorensen(val1, val2)(1)
    } yield s
    val minJoinSim = if (sims.isEmpty) 0.0 else sims.min
    
    val fbidPairs = for {
      (key1, key2) <- query.joinPairs
      fbid1 <- freebaseLink(key1, tuple)
      fbid2 <- freebaseLink(key2, tuple)
    } yield (fbid1, fbid2)
    
    val fbidViolation = if (fbidPairs.exists(pair => pair._1 != pair._2)) 1.0 else 0.0
    
    SparseVector(
        "minimum join key similarity" -> minJoinSim,
        "fbid join key violation" -> fbidViolation)
  }
  
  val templateFeatures = TemplatePairFeature { (q: String, pair: TemplatePair) => {
    val prefix1 = NlpUtils.questionPrefix(pair.template1)
    val prefix2 = NlpUtils.questionPrefix(pair.template2)
    SparseVector(s"template prefix $prefix1 => $prefix2" -> 1.0,
    			 "template pair pmi" -> pair.pmi,
    			 "template pair count1" -> pair.count1,
    			 "template pair count2" -> pair.count2,
    			 "template pair count12" -> pair.count12,
    			 "template is typed" -> {if (pair.typ == "anything") 0.0 else 1.0}) 
  }
  }
  
  val numSteps = (step: QaStep) => SparseVector("steps" -> 0.25)
  
  def paraphraseLm(step: QaStep): SparseVector = {
    (step.action, step.toState) match {
      case (a: TemplatePair, qs: QuestionState) if qs.isParaphrased => ("paraphrase lm", lmClient.query(qs.question))
      case _ => SparseVector.zero
    }
  }
  
  def templateArgFeatures(step: QaStep): SparseVector = step.toState match {
    case s: AbstractedArgState => {
      val sent = s.processed
      val span = s.argInterval
      val tags = sent.postags.slice(span.start, span.end)
      val tagPat = tags.mkString(" ")
      Some(s"template arg pos tags = $tagPat")
    }
    case _ => SparseVector.zero
  }
  
  private val defNoun = "^[Tt]he [a-z].*$".r
  val isDefiniteNoun = ExecutionFeature { (q: String, etuple: ExecTuple) =>
    val answer = etuple.answerString
    if (defNoun.findFirstIn(answer).isDefined)
      SparseVector("answer is def noun" -> 1.0)
    else
      SparseVector.zero
  }
  
  val prefixAndFeat = ExecutionFeature { (q: String, etuple: ExecTuple) =>
    val a = etuple.answerString
    val prefix = NlpUtils.questionPrefix(q)
    val isDate = if (NlpUtils.isDate(a)) 1.0 else 0.0
    val isNumber = if (NlpUtils.containsNumber(a)) 1.0 else 0.0
    val shape = NlpUtils.stringShape(a, 4)
    SparseVector(
      s"question prefix = '$prefix' ^ isDate" -> isDate,
      s"question prefix = '$prefix' ^ isNumber" -> isNumber,
      s"question prefix = '$prefix' ^ answer shape = '$shape'" -> 1.0
    )
  }
  
  val lightVerbRel = QueryFeature { (q: String, query: ConjunctiveQuery) =>
    val values = for {
      c <- query.conjuncts
      (field, literal) <- c.literalFields
      value = literal.value
      if field == Search.rel && NlpUtils.isLightVerb(value)
    } yield value
    if (values.isEmpty) {
      None
    } else {
      Some(s"query relation is light verb")
    }
  }
  
  val relSynFeatures = (step: QaStep) => step.action match {
    case r: RelSynRule => SparseVector("relSyn pmi" ->r.pmi)
    case _ => SparseVector.zero
  }
  
  val paraRuleFeatures = (step: QaStep) => step.action match {
    case r: ParaphraseRule => SparseVector(r.name -> 1.0)
    case _ => SparseVector.zero
  }
  
  val parserFeatures = (step: QaStep) => step.action match {
    case parse: ParsedQuestion => {
      val deriv = parse.derivation
      val lexRules = deriv.terminals.map(t => s"parser lexical rule = ${t.rule}")
      val lexRuleContexts = deriv.terminals.flatMap { t =>
        val i = t.interval.start
        val j = t.interval.end
        val n = parse.question.postags.size
        val r = t.category.categoryString
        val leftTag = if (i > 0) parse.question.postags(i-1) else "<s>"
        val rightTag = if (j < n - 2) parse.question.postags(j+1) else "</s>"
        Map(s"lex type = $r ^ leftTag = $leftTag" -> 1.0, s"lex type = $r ^ rightTag = $rightTag" -> 1.0)
      }
      val combRules = deriv.combinators.map(c => s"parser combinator rule = $c")
      val usesFull = if (deriv.terminals.exists(t => t.rule.toString.startsWith("fullPattern"))) 1.0 else 0.0
      val posLexRules = deriv.terminals.map { t =>
        val i = t.catspan.span
        val tags = parse.postags(i)
        s"lex category (postags) = ${t.catspan.category.categoryString}($tags)"
      }
      val counts = (lexRules ++ combRules ++ posLexRules).groupBy(x => x).map {
        case (name, names) => (name -> 1.0)
      }
      SparseVector(counts) + lexRuleContexts + ("num lexical rules" -> lexRules.size / 5.0) + ("uses full parser pattern" -> usesFull)
    }
    case _ => SparseVector.zero
  }
  
  def apply(s: QaStep) = actionType(s) +
		  				 answerIsLinked(s) +
		  				 tupleNamespace(s) +
		  				 querySimilarity(s) +
		  				 templateFeatures(s) +
		  				 paraphraseLm(s) +
		  				 numConjuncts(s) +
		  				 prefixAndFeat(s) +
		  				 lightVerbRel(s) +
		  				 relSynFeatures(s) +
		  				 templateArgFeatures(s) +
		  				 joinSimilarity(s) +
		  				 parserFeatures(s) + 
		  				 paraRuleFeatures(s) +
		  				 numSteps(s) +
		  				 isDefiniteNoun(s) +
		  				 tupleFeatures(s)
  
}

case class TemplatePairFeature(f: Function2[String, TemplatePair, SparseVector]) extends Function[QaStep, SparseVector] {
  override def apply(step: QaStep) = step.action match {
    case a: TemplatePair => f(step.question, a)
    case _ => SparseVector.zero
  }
}

case class ExecutionFeature(f: Function2[String, ExecTuple, SparseVector]) extends Function[QaStep, SparseVector] {
  override def apply(step: QaStep) = step.toState match {
    case ts: TupleState => f(step.question, ts.execTuple)
    case _ => SparseVector.zero
  }
}

case class QueryFeature(f: Function2[String, ConjunctiveQuery, SparseVector]) extends Function[QaStep, SparseVector] {
  override def apply(step: QaStep) = step.toState match {
    case qs: QueryState => f(step.question, qs.query)
    case _ => SparseVector.zero
  }
}

case class ActionFeature(f: QaAction => SparseVector) extends Function[QaStep, SparseVector] {
  override def apply(step: QaStep) = f(step.action)
}