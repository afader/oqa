package edu.knowitall.relsyn

import edu.knowitall.execution.TConjunct
import edu.knowitall.execution.Search
import edu.knowitall.execution.TLiteral
import edu.knowitall.execution.QuotedTLiteral
import edu.knowitall.search.qa.QaAction
import edu.knowitall.execution.UnquotedTLiteral

case class RelSynRule(rel1: String, rel2: String, inverted: Boolean, 
    count1: Double, count2: Double, jointCount: Double, pmi: Double) 
    extends QaAction {
  
  private def swapArgs(c: TConjunct) = for {
    a1 <- c.values.get(Search.arg1)
    a2 <- c.values.get(Search.arg2)
    newvals = c.values ++ List((Search.arg1 -> a2), (Search.arg2 -> a1))
  } yield c.copy(values = newvals)
  
  private def adjustSwap(c: TConjunct) = if (inverted) swapArgs(c) else Some(c)
  
  private def replaceRel(c: TConjunct) = {
    val newr = UnquotedTLiteral(rel2)
    Some(c.copy(values = c.values + (Search.rel -> newr)))
  }
  
  private def relValue(c: TConjunct) = for {
    value <- c.values.get(Search.rel)
    svalue <- value match {
      case l: TLiteral => Some(l.value)
      case _ => None
    }
  } yield svalue
  
  def apply(c: TConjunct) = for {
    svalue <- relValue(c)
    withNewRel <- replaceRel(c)
    newc <- adjustSwap(withNewRel)
  } yield newc
  
  def serialize = List(rel1, rel2, inverted, count1, count2, jointCount, pmi).mkString("\t")
  
}

case object RelSynRule {
  def deserialize(line: String) = line.trim.split("\t").toList match {
    case rel1 :: rel2 :: inverteds :: count1s :: count2s :: jointCounts :: pmis :: Nil =>
      RelSynRule(rel1, rel2, inverteds.toBoolean, count1s.toDouble,
        count2s.toDouble, jointCounts.toDouble, pmis.toDouble)
    case _ => throw new IllegalArgumentException(s"Invalid RelSynRule: $line")
  }
}