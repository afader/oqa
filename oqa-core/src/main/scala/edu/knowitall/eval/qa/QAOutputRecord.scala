package edu.knowitall.eval.qa

import edu.knowitall.eval.OutputRecord
import edu.knowitall.execution.Tuple
import edu.knowitall.execution.Search

case class QAOutputRecord(question: String, answer: String, ascore: Double, derivation: String) extends OutputRecord {
  override def toString = s"$question\t$answer\t$score\t$derivation"
  override def input = question
  override def output = answer
  override def score = ascore
}
case object QAOutputRecord {
  def fromLine(line: String) = line.trim.split("\t") match {
    case Array(q, a, s, d) => QAOutputRecord(q, a, s.toDouble, d)
    case Array(q, a, s) => QAOutputRecord(q, a, s.toDouble, "")
    case _ => throw new IllegalArgumentException(s"Could not parse line: $line")
  }
  def project(t: Tuple): String = {
    val tup = Search.ProjectTriples(List(t)).toList(0)
    tup.toString
  }
}
