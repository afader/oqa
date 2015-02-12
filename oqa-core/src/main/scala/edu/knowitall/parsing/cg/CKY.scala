package edu.knowitall.parsing.cg

import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.collection.immutable.Interval
import scala.collection.mutable.{Map => MutableMap}
import edu.knowitall.util.MathUtils

sealed trait Node {
  def span: Interval
  def category: Category
}

case class CatSpan(category: Category, span: Interval)

case class Terminal(catspan: CatSpan,
					   rule: TerminalRule) extends Node {
  override val span = catspan.span
  override val category = catspan.category
}

case class NonTerminal(catspan: CatSpan, left: CatSpan, right: CatSpan, 
    rule: Combinator) extends Node {
  override val span = catspan.span
  override val category = catspan.category
}

sealed trait Derivation {
  def catspan: CatSpan
  def category = catspan.category
  def interval = catspan.span
  def terminals: List[LexicalStep]
  def combinators: List[Combinator]
}

case class CombinatorStep(catspan: CatSpan, rule: Combinator, left: Derivation, right: Derivation) extends Derivation {
  override def combinators = rule :: (left.combinators ++ right.combinators)
  override def terminals = left.terminals ++ right.terminals
}

case class LexicalStep(catspan: CatSpan, rule: TerminalRule) extends Derivation {
  override def combinators = Nil
  override def terminals = List(this)
  override def toString = s"$rule => $interval $category"
}

case class CKY(input: Sentence with Chunked with Lemmatized, size: Int, 
				  terminalRules: IndexedSeq[TerminalRule],
				  combinators: IndexedSeq[Combinator]) {
  
  val cats = MutableMap.empty[Interval, Set[Category]]
  val nodes = MutableMap.empty[CatSpan, Node]
  
  private def applyTerminalRules = for {
    interval <- MathUtils.allIntervals(size)
    rule <- terminalRules
    category <- rule(interval, input)
    catspan = CatSpan(category, interval)
    terminal = Terminal(catspan, rule)
  } yield {
    cats += (interval -> (cats.getOrElse(interval, Set.empty) + category))
    nodes += (catspan -> terminal)
  }
  
  private def applyCombinators(length: Int) = for {
    interval <- MathUtils.intervals(length, size)
    (left, right) <- MathUtils.splits(interval)
    lcat <- cats.getOrElse(left, Set.empty)
    rcat <- cats.getOrElse(right, Set.empty)
    combinator <- combinators
    cat <- combinator(lcat, rcat)
    node = NonTerminal(CatSpan(cat, interval), CatSpan(lcat, left), CatSpan(rcat, right), combinator)
  } {
    cats += (interval -> (cats.getOrElse(interval, Set.empty) + cat))
    nodes += (CatSpan(cat, interval) -> node)
  }
  
  def parse = {
    applyTerminalRules
    for (length <- 2 to size) applyCombinators(length)
  }
  
  private val fullSpan = Interval.open(0, size)
  
  def rootCategories = cats.getOrElse(fullSpan, Set())
  
  def derivations(catspan: CatSpan): Iterable[Derivation] = nodes.get(catspan) match {
    case None => Iterable.empty
    case Some(node) => node match {
      case Terminal(cs, rule) => Iterable(LexicalStep(catspan, rule))
      case NonTerminal(cs, left, right, rule) => for {
        leftd <- derivations(left)
        rightd <- derivations(right)
      } yield CombinatorStep(catspan, rule, leftd, rightd)
    }
  }
  
  def rootDerivations = for {
    c <- cats.getOrElse(fullSpan, Set.empty)
    cs = CatSpan(c, fullSpan)
    d <- derivations(cs)
  } yield d
  
}