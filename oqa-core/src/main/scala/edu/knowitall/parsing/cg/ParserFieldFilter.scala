package edu.knowitall.parsing.cg

import edu.knowitall.execution.Search
import edu.knowitall.execution.TLiteral
import java.io.InputStream
import scala.io.Source

class ParserFieldFilter(field: Search.Field, pattern: String) extends Function[ParsedQuestion, Boolean] {
  private val pat = pattern.r 
  override def apply(pq: ParsedQuestion): Boolean = {
    val values = for {
      c <- pq.query.conjuncts
      (f, v) <- c.values collect { case (a, b: TLiteral) => (a, b.value) }
      if f == field
    } yield v
    val hasMatch = values.exists(pat.findFirstIn(_).isDefined)
    !hasMatch
  }
}

case class ParserRelFilter(pattern: String) extends ParserFieldFilter(Search.rel, pattern)
case object ParserRelFilter {
  def fromInputStream(in: InputStream) = ParserFieldFilter.fromInputStream(Search.rel, in)
}

case class ParserArgFilter(pattern: String) extends Function[ParsedQuestion, Boolean] {
  val p1 = new ParserFieldFilter(Search.arg1, pattern)
  val p2 = new ParserFieldFilter(Search.arg2, pattern)
  override def apply(pq: ParsedQuestion): Boolean = p1(pq) && p2(pq)
}
case object ParserArgFilter {
  def fromInputStream(in: InputStream) = {
    val lines = Source.fromInputStream(in, "UTF-8").getLines.toList.map(ParserFieldFilter.removeComment).filter(_ != "")
    lines.map(ParserArgFilter(_))
  }
}

object ParserFieldFilter {
  def removeComment(line: String) = line.replaceAll("#.*", "").replaceAll("//.*", "").trim 
  def fromLines(field: Search.Field, lines: Iterable[String]) = for {
    line <- lines
    replaced = removeComment(line)
    if replaced != ""
  } yield new ParserFieldFilter(field, replaced)
  def fromInputStream(field: Search.Field, in: InputStream) = fromLines(field, Source.fromInputStream(in, "UTF-8").getLines.toIterable)
}