package edu.knowitall.execution

import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.tool.stem.MorphaStemmer
import scala.io.Source
import java.io.InputStream
import org.slf4j.LoggerFactory
import scala.util.matching.Regex
import edu.knowitall.util.ResourceUtils

trait AnswerFilter {
  def filter(exec: QueryExecutor): QueryExecutor
}

object AnswerFilter {
  // Mnemonics
  type QE = QueryExecutor
  type ETPred = (ExecTuple => Boolean)
  type SPred = (String => Boolean)
  
  def createExecTupleFilter(f: ETPred): (QE => QE) = exec => new QE {
    def execute(q: ConjunctiveQuery) = exec.execute(q).filter(f)
  }
  
  def createStringFilter(f: SPred): (QE => QE) = {
    val etpred = (et: ExecTuple) => f(et.answer.mkString(", "))
    createExecTupleFilter(etpred)
  }
  
  def compose(af1: AnswerFilter, af2: AnswerFilter) = new AnswerFilter {
    override def filter(exec: QueryExecutor) = af2.filter(af1.filter(exec))
  }
}

case class StopListFilter(stops: Iterable[String]) extends AnswerFilter {
  val tokenizer = new ClearTokenizer()
  val logger = LoggerFactory.getLogger(this.getClass)
  logger.debug(s"stop set: $stops")
  val stopSet = stops.map(normalize).toSet
  def normalize(s: String): String = { 
    val slc = s.toLowerCase()
    val toks = tokenizer(slc)
    val stems = toks.map(MorphaStemmer.stemToken)
    val lems = stems.map(_.lemma)
    val result = lems.mkString(" ")
    result
  }
  def this(file: String) = this(Source.fromFile(file, "UTF8").getLines.toList)
  def this(is: InputStream) = this(Source.fromInputStream(is, "UTF8").getLines.toList)
  def keepAnswer(ans: String) = !stopSet.contains(normalize(ans))
  override def filter(exec: QueryExecutor): QueryExecutor = 
    AnswerFilter.createStringFilter(keepAnswer)(exec)
}

case class StopRegexFilter(patStrs: Iterable[String]) extends AnswerFilter {
  val pats = patStrs.toList.map(_.r)
  def this(file: String) = this(Source.fromFile(file, "UTF8").getLines.toList)
  def this(is: InputStream) = this(Source.fromInputStream(is, "UTF8").getLines.toList)
  def keepAnswer(ans: String) = !pats.exists(p => ans match {
    case p() => true
    case _ => false
  })
  override def filter(exec: QueryExecutor) = AnswerFilter.createStringFilter(keepAnswer)(exec)
}

object DefaultFilters {
  val logger = LoggerFactory.getLogger(this.getClass)
  
  val idFilter = new AnswerFilter {
    override def filter(exec: QueryExecutor) = exec
  }
  
  def wrap(executor: QueryExecutor) = {
    val filters: List[AnswerFilter] = List(
      new StopListFilter(ResourceUtils.resource("/edu/knowitall/execution/stoplist.txt")),
      new StopRegexFilter(ResourceUtils.resource("/edu/knowitall/execution/stopregex.txt")))
  
    val composed = filters.foldLeft(idFilter)(AnswerFilter.compose _)
    composed.filter(executor)
  }
  
}
