package edu.knowitall.search

trait SearchProblem[State, Action] {
  def initialState: State
  def successors(s: State): Iterable[(Action, State)]
  def isGoal(s: State): Boolean
  def cost(from: State, action: Action, to: State): Double
}