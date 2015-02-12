package edu.knowitall.relsyn

import scala.io.Source
import java.io.InputStream
import edu.knowitall.execution.TConjunct
import edu.knowitall.execution.Search
import edu.knowitall.execution.UnquotedTLiteral
import edu.knowitall.execution.QuotedTLiteral

case class ListRelSynClient(rules: List[RelSynRule]) extends RelSynClient {
  private def rulesFor(rel: String) = rules.filter(_.rel1 == rel.toLowerCase)
  override def relSyns(c: TConjunct) = { 
    c.values.get(Search.rel) match {
      case Some(UnquotedTLiteral(l)) => rulesFor(l)
      case Some(QuotedTLiteral(l)) => rulesFor(l)
      case _ => List.empty
    } 
  }
}

case object ListRelSynClient {
  def fromInputStream(is: InputStream) = ListRelSynClient(Source.fromInputStream(is).getLines.map(RelSynRule.deserialize).toList)
}