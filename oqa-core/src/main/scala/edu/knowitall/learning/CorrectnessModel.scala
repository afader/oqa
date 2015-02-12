package edu.knowitall.learning

trait CorrectnessModel[Input, Output] {
  def isCorrect(input: Input, output: Output): Boolean
  def pickCorrect(input: Input, candidates: Seq[Output]): Option[Output]
}