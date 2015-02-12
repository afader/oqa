package edu.knowitall.search

trait Transition[State, Action] extends Function[State, Iterable[(Action, State)]] {
  def +(other: Transition[State, Action]) = Transition.union(this, other)
}

object Transition {
  def union[State, Action](t1: Transition[State, Action],
      t2: Transition[State, Action]) = new Transition[State, Action] {
    override def apply(s: State) = t1(s) ++ t2(s)
  }
}