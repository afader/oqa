package edu.knowitall.parsing.cg

import com.typesafe.config.ConfigFactory
import java.io.FileInputStream
import java.io.File
import edu.knowitall.util.ResourceUtils
import edu.knowitall.util.NlpTools
import edu.knowitall.tool.chunk.Chunker
import edu.knowitall.tool.stem.Stemmer
import edu.knowitall.execution.ConjunctiveQuery
import edu.knowitall.search.qa.QaAction
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.execution.TLiteral

case class ParsedQuestion(question: Sentence with Chunked with Lemmatized,
    query: ConjunctiveQuery, derivation: Derivation) extends QaAction {
  
  private def sliceString[A](seq: Traversable[A], i: Interval) = 
    seq.slice(i.start, i.end).mkString(" ")
  
  def postags(i: Interval) = sliceString(question.postags, i)
  
}

case class CgParser(lexicon: IndexedSeq[LexicalRule] = CgParser.defaultLexicon, 
					combinators: IndexedSeq[Combinator] = CgParser.defaultCombinators,
					chunker: Chunker = NlpTools.dummyChunker,
					lemmatizer: Stemmer = NlpTools.stemmer,
					maxConjuncts: Int = CgParser.defaultMaxConjuncts,
					outputFilter: ParsedQuestion => Boolean = CgParser.defaultOutputFilter) {
 
  private def process(s: String) = NlpTools.process(s, chunker, lemmatizer)
 
  private def getQuery(cat: Category) = cat match {
    case Unary(freeVar, query, _) => Some(query)
    case _ => None
  }
  
  def parse(s: String) = {
    val sent = process(s)
    val n = sent.tokens.size
    val cky = new CKY(sent, n, lexicon, combinators)
    cky.parse
    for {
      derivation <- cky.rootDerivations
      query <- derivation.category match {
        case u: Unary => Some(u.query)
        case _ => None
      }
      output = ParsedQuestion(sent, query, derivation)
      if output.query.conjuncts.size <= maxConjuncts
      if outputFilter(output)
    } yield ParsedQuestion(sent, query, derivation)
  }
  
  def apply(s: String) = parse(s)

}

case object CgParser {
  val conf = ConfigFactory.load()
  val defaultCombinators = IndexedSeq(RightApply, LeftApply, UnaryIntersect,
		  							  UnaryIdentity, ApplyMod)
  lazy val lexiconIn = if (conf.hasPath("parsing.cg.lexiconPath")) {
    new FileInputStream(new File(conf.getString("parsing.cg.lexiconPath")))
  } else {
    ResourceUtils.resource(conf.getString("parsing.cg.lexiconClasspath"))
  }
  
  lazy val ruleKeep = conf.getString("parsing.cg.lexicalRuleKeep").r
  lazy val ruleSkip = conf.getString("parsing.cg.lexicalRuleSkip").r
  
  lazy val defaultLexicon = LexicalRule.fromInputStream(lexiconIn).filter {
    rule => ruleKeep.findPrefixMatchOf(rule.name).isDefined &&
    	   !ruleSkip.findPrefixMatchOf(rule.name).isDefined
  }
  
  lazy val defaultMaxConjuncts = conf.getInt("parsing.cg.maxConjuncts")
  
  lazy val defaultOutputFilter = {
    val relIn = ResourceUtils.resource("/edu/knowitall/parsing/cg/relFilters.txt")
    val rels = ParserRelFilter.fromInputStream(relIn)
    val argIn = ResourceUtils.resource("/edu/knowitall/parsing/cg/argFilters.txt")
    val args = ParserArgFilter.fromInputStream(argIn)
    (rels ++ args).reduce((a, b) => (x => a(x) && b(x)))
  }
  
}