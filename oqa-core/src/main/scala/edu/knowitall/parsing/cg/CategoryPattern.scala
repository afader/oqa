package edu.knowitall.parsing.cg

import edu.knowitall.execution.TVariable
import edu.knowitall.execution.ListConjunctiveQuery
import edu.knowitall.execution.UnquotedTLiteral
import edu.knowitall.execution.Search.rel
import edu.knowitall.execution.FieldIndex

trait CategoryPattern {
  def apply(bindings: Map[TVariable, String]): Option[Category]
}

object CategoryPattern {
  def fromString(s: String) = s.trim().split("\\s+", 2) match {
    case Array("identity") => IdentityPattern
    case Array("unary", pattern) => UnaryPattern(pattern)
    case Array("binary", pattern) => BinaryPattern(pattern)
    case Array("argument", pattern) => ArgumentPattern(pattern)
    case Array("mod", pattern) => ModPattern(pattern)
    case _ => throw new IllegalArgumentException(s"Invalid pattern string: $s")
  }
}

case class UnaryPattern(pattern: String) extends CategoryPattern {
  private val cqp = ConjunctiveQueryPattern(pattern)
  assume(cqp.boundVars.size == 1, s"UnaryPattern $pattern must have 1 bound variable")
  val freeVar = cqp.query.qVars(0)
  override def apply(bindings: Map[TVariable, String]) = for {
    query <- cqp(bindings)
  } yield Unary(freeVar, query)
}

case class BinaryPattern(pattern: String) extends CategoryPattern {
  private val cqp = ConjunctiveQueryPattern(pattern)
  assume(cqp.boundVars.size == 2, s"BinaryPattern $pattern must have 2 bound variables")
  val leftVar = cqp.query.qVars(0)
  val rightVar = cqp.query.qVars(1)
  override def apply(bindings: Map[TVariable, String]) = for {
    query <- cqp(bindings)
    relFields = for {
      c <- query.conjuncts
      (field, value) <- c.values
      if field == rel
    } yield FieldIndex(c.name, field)
  } yield Binary(leftVar, rightVar, query, relFields.toSet)
}

case class ArgumentPattern(pattern: String) extends CategoryPattern {
  private val sp = UnquotedTLiteral(pattern)
  override def apply(bindings: Map[TVariable, String]) = {
    val valBindings = bindings map { case (k, v) => (k, UnquotedTLiteral(v)) }
    Some(Arg(sp.subs(valBindings)))
  }
}

case class ModPattern(pattern: String) extends CategoryPattern {
  private val sp = UnquotedTLiteral(pattern)
  override def apply(bindings: Map[TVariable, String]) = {
    val valBindings = bindings map { case (k, v) => (k, UnquotedTLiteral(v)) }
    Some(Mod(sp.subs(valBindings).toString))
  }
}

object IdentityPattern extends CategoryPattern {
  override def apply(bindings: Map[TVariable, String]) = Some(Identity)
}

case class ConjunctiveQueryPattern(pattern: String) {
  
  val query = ListConjunctiveQuery.fromString(pattern) match {
    case Some(x) => x
    case None => throw new IllegalArgumentException(s"Invalid pattern: $pattern")
  }
  
  val boundVars = query.qVars
    
  val freeVars = query.conjuncts.flatMap(_.vars).toSet -- boundVars
  
  def apply(bindings: Map[TVariable, String]) = {
    if (freeVars.subsetOf(bindings.keys.toSet)) {
      val literals = bindings map { case (tvar, s) => (tvar, UnquotedTLiteral(s)) }
      Some(query.subs(literals))
    } else {
      None
    }
  }
  
}