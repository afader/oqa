package edu.knowitall.parsing.cg

import edu.knowitall.execution.TLiteral
import edu.knowitall.execution.TVariable
import edu.knowitall.execution.ConjunctiveQuery
import edu.knowitall.execution.FieldIndex
import edu.knowitall.execution.TVal
import edu.knowitall.execution.UnquotedTLiteral

trait Category {
  def categoryString: String
}

case class Arg(value: TLiteral) extends Category {
  override val categoryString = "Arg"
}

case class Unary(freeVar: TVariable, query: ConjunctiveQuery, modFields: Set[FieldIndex] = Set.empty) extends Category {
  
  override val categoryString = "Unary"
  
  private def renameFieldIndex(prefix: String, index: FieldIndex) = {
    val i = query.conjuncts.indexWhere(c => c.name == index.conjunctName)
    assume(i >= 0, s"field index $index not in query $query")
    index.copy(conjunctName = s"${prefix}.$i")
  }
  
  def renameConjuncts(prefix: String): Unary = {
    val newFields = modFields.map(renameFieldIndex(prefix, _))
    val newQuery = query.renameConjuncts(prefix)
    Unary(freeVar, newQuery, newFields)
  }
  
  def intersect(that: Unary): Unary = {
    val u1 = this.renameConjuncts("r")
    val u2 = that.renameConjuncts("s")
    val newVar = TVariable(u1.freeVar.name + u2.freeVar.name)
    val oldVar1 = u1.freeVar
    val oldVar2 = u2.freeVar
    val query1 = u1.query.subs(oldVar1, newVar)
    val query2 = u2.query.subs(oldVar2, newVar)
    val newQuery = query1.combine(query2).subs(newVar, Unary.finalVar)
    Unary(newVar, newQuery, u1.modFields ++ u2.modFields)
  }
}
case object Unary {
  val finalVar = TVariable("x")
}

case class Binary(leftVar: TVariable, rightVar: TVariable, 
    query: ConjunctiveQuery, modFields: Set[FieldIndex] = Set.empty) extends Category {
  
  override val categoryString = "Binary"
  
  def leftApply(a: Arg): Unary = {
    val newQuery = query.subs(leftVar, a.value)
    Unary(rightVar, newQuery, modFields)
  }
  
  def rightApply(a: Arg): Unary = {
    val newQuery = query.subs(rightVar, a.value)
    Unary(leftVar, newQuery, modFields)
  }
  
}

case class Mod(value: String) extends Category {
  
  override val categoryString = "Mod"
  
  private def updateValue(v: TVal) = v match {
    case l: TLiteral => l.update(s"${l.value} $value")
    case _ => v
  }
  
  private def modifyFields(is: List[FieldIndex], q: ConjunctiveQuery): ConjunctiveQuery = is match {
    case Nil => q
    case i :: rest => modifyFields(rest, i.updateQuery(q, updateValue))
  } 
  
  def modify(u: Unary): Option[Unary] = {
    val newQuery = modifyFields(u.modFields.toList, u.query)
    if (newQuery == u.query) {
      None
    } else {
      Some(u.copy(query = newQuery))
    }
  }
  
  override def toString = s"Mod($value)"
}
    
object Identity extends Category {
  override val categoryString = "Identity"
  override def toString = "Identity"
}