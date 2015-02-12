package edu.knowitall.parsing.cg

import edu.knowitall.execution.ListConjunctiveQuery
import edu.knowitall.execution.UnquotedTLiteral
import edu.knowitall.execution.TVariable
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.tool.typer.Type

trait Combinator {
  def apply(left: Category, right: Category): Option[Category]
}

trait TerminalRule {
  def apply(interval: Interval, sent: Sentence with Chunked with Lemmatized): Option[Category]
}

object RightApply extends Combinator {
  override def apply(left: Category, right: Category) = (left, right) match {
    case (b: Binary, a: Arg) => Some(b.rightApply(a))
    case _ => None 
  }
  override def toString = "RightApply"
}

object LeftApply extends Combinator {
  override def apply(left: Category, right: Category) = (left, right) match {
    case (a: Arg, b: Binary) => Some(b.leftApply(a))
    case _ => None 
  }
  override def toString = "LeftApply"
}

object UnaryIntersect extends Combinator {
  override def apply(left: Category, right: Category) = (left, right) match {
    case (u1: Unary, u2: Unary) => {
      Some(u1.intersect(u2))
    }
    case _ => None
  }
  override def toString = "UnaryIntersect"
}

object UnaryIdentity extends Combinator {
  override def apply(left: Category, right: Category) = (left, right) match {
    case (Identity, u: Unary) => Some(u)
    case (u: Unary, Identity) => Some(u)
    case _ => None
  }
  override def toString = "UnaryIdentity"
}

object ApplyMod extends Combinator {
  override def apply(left: Category, right: Category) = (left, right) match {
    case (m: Mod, u: Unary) => m.modify(u)
    case _ => None
  }
  override def toString = "Mod"
} 