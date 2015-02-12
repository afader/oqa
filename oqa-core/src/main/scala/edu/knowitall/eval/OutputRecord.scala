package edu.knowitall.eval

abstract class OutputRecord {
  def input: String
  def output: String
  def score: Double
  override def toString = s"$input\t$output\t$score"
}

object OutputRecord {
  
  private case class OutputRecordImpl(input: String, output: String, 
      score: Double) extends OutputRecord {
    def apply(input: String, output: String, score: Double) =
      OutputRecordImpl(input, output, score)
  }
  
  def apply(input: String, output: String, score: Double): OutputRecord = OutputRecordImpl(input, output, score)
  
  def fromLine(line: String): OutputRecord = line.split("\t").toList match {
    case i :: o :: s :: rest => OutputRecordImpl(i, o, s.toDouble)
    case _ => throw new IllegalArgumentException(s"Could not parse line: $line")
  }

}
