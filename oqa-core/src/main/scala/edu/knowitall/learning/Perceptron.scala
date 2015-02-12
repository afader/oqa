package edu.knowitall.learning

class Perceptron[Input, Output](model: HiddenVariableModel[Input, Output], 
    oracle: CorrectnessModel[Input, Output]) {
  
  def learnIter(input: Input) = for {
    prediction <- model.predict(input)
    if !oracle.isCorrect(input, prediction)
    candidates = model.candidatePredictions(input)
    correct <- oracle.pickCorrect(input, candidates)
  } model.update(input, prediction, correct)
  
  def learn(inputs: Traversable[Input]) = inputs foreach learnIter
  
}