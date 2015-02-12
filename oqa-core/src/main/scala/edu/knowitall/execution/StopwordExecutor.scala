package edu.knowitall.execution

case class StopwordExecutor(baseExecutor: QueryExecutor) extends QueryExecutor {
  
  import java.util.regex.Pattern
  
  val stops = Set("a", "an", "the", "'s", "these", "those", "some", "that", "something")
  
  override def execute(q: ConjunctiveQuery): Iterable[ExecTuple] = 
    baseExecutor.execute(cleanQuery(q))
  
  def cleanQuery(q: ConjunctiveQuery): ListConjunctiveQuery = {
    ListConjunctiveQuery(q.qVars, q.conjuncts map cleanConjunct)
  }

  def cleanConjunct(c: TConjunct): TConjunct = {
    val cleanValues = c.values.map {
      case (key, UnquotedTLiteral(value)) =>
        (key, UnquotedTLiteral(cleanWords(value)))
      case (key, value) => (key, value)
    }
    c.copy(values = cleanValues)
  }
  
  def cleanWords(str: String) = {
    val cleaned = str.split(" ").filterNot(stops.contains).mkString(" ")
    if (cleaned.isEmpty) str else cleaned
  }
}