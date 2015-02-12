package edu.knowitall.execution
import edu.knowitall.execution.Search.Field
import edu.knowitall.triplestore.TriplestoreClient
import org.slf4j.LoggerFactory
import edu.knowitall.tool.stem.MorphaStemmer
import scala.collection.immutable.SortedMap

case class ExecTuple(tuple: Tuple, query: ConjunctiveQuery) {
  val answer: List[String] = query.qAttrs.flatMap(a => tuple.getString(a))
  val answerString: String = answer match {
    case List(a) => a
    case _ => "(" + answer.mkString(", ") + ")"
  }
  val toTripleString = {
    val tstrs = for {
      c <- query.conjuncts
      n = c.name
      x = tuple.attrs.getOrElse(s"$n.arg1", "")
      r = tuple.attrs.getOrElse(s"$n.rel", "")
      y = tuple.attrs.getOrElse(s"$n.arg2", "")
    } yield s"($x, $r, $y)"
    tstrs.mkString(" ")
  }
}

trait QueryExecutor {
  def execute(query: ConjunctiveQuery): Iterable[ExecTuple]
}

case class IdentityExecutor(client: TriplestoreClient) extends QueryExecutor {

  val logger = LoggerFactory.getLogger(this.getClass)

  val joiner = Joiner(client)

  override def execute(q: ConjunctiveQuery): Iterable[ExecTuple] = {
    val joined = joiner.joinQueries(q.conjuncts)
    for (t <- joined; et = ExecTuple(t, q)) yield et
  }
}