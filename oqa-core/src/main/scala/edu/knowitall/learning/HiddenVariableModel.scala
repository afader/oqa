package edu.knowitall.learning

trait HiddenVariableModel[Input, Output] {
  def predict(input: Input): Option[Output]
  def candidatePredictions(input: Input): Seq[Output]
  def update(input: Input, predicted: Output, expected: Output): Unit
}