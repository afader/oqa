package edu.knowitall.execution

import Search.Field
import Search.TSQuery
import Search.{FieldKeywords, FieldPhrase}
import Search.Conjunction
import org.slf4j.LoggerFactory
import Search._
import scala.Option.option2Iterable
import edu.knowitall.util.StringUtils

/**
 * Base trait for triple values.
 */
trait TVal

/**
 * Base trait for triple literal values.
 */
trait TLiteral extends TVal {
  def value: String
  def update(v: String): TLiteral
  def toConjunct(field: Field): TSQuery
  def subs(binding: Map[TVariable, TVal]): TLiteral
}

/**
 * Triple literals can be unquoted, which has the semantics of doing a keyword 
 * search over a field.
 */
case class UnquotedTLiteral(value: String) extends TVal with TLiteral {
  override def toString = value
  override def toConjunct(field: Field) = FieldKeywords(field, value)
  override def subs(binding: Map[TVariable, TVal]) = {
    val map = binding map { case (k, v) => (k.name, v.toString) }
    val newVal = StringUtils.interpolate(value, map)
    copy(value = newVal)
  }
  override def update(v: String) = UnquotedTLiteral(v)
}

/**
 * Triple literals can also be quoted, which has the semantics of doing an
 * exact-match search over a field.
 */
case class QuotedTLiteral(value: String) extends TVal with TLiteral {
  override def toString = s""""$value""""
  override def toConjunct(field: Field) = FieldPhrase(field, value)
  override def subs(binding: Map[TVariable, TVal]) = {
    val map = binding map { case (k, v) => (k.name, v.toString) }
    val newVal = StringUtils.interpolate(value, map)
    copy(value = newVal)
  }
  override def update(v: String) = QuotedTLiteral(v)
}
case object QuotedTLiteral {
  val quoted = """^"(.*)"$""".r
  def fromString(s: String): Option[QuotedTLiteral] = s.trim() match {
    case quoted(s) => Some(QuotedTLiteral(s))
    case _ => None
  }
}

/**
 * Triple values can also be variables, which have a string name.
 */
case class TVariable(name: String) extends TVal {
  override def toString = "$" + name
}
case object TVariable {
  val vpat = """\$([A-Za-z0-9_]+)""".r
  def fromString(s: String): Option[TVariable] = s.trim() match {
    case vpat(v) => Some(TVariable(v))
    case "?" => Some(TVariable("?"))
    case _ => None
  }
  def fromStringMult(s: String): List[TVariable] = {
    val parts = s.split(", *").toList
    for (p <- parts; t = TVariable.fromString(p)) yield t match {
      case Some(x) => x
      case None => throw new 
        IllegalArgumentException(s"Could not parse variables in: $s")
    }
  }
}

/**
 * TConjunct objects have a name (you can think of this as a unique
 * identifier for the relational table returned) and map containing the
 * field values. The field values map a field (arg1, rel, or arg2) to a 
 * value (literal or variable).
 */
case class TConjunct(name: String, values: Map[Field, TVal]) {  
  
  def literalFields: Iterable[(Field, TLiteral)] = { 
    for ((f, v) <- values) yield v match {
      case l: TLiteral => Some((f, l))
      case _ => None
    }
  }.flatten
  
  def variableFields: Iterable[(Field, TVariable)] = {
    for ((f, v) <- values) yield v match {
      case TVariable(s) => Some((f, TVariable(s)))
      case _ => None
    }
  }.flatten
  
  def varsToFields: Map[TVariable, Field] = variableFields.map(_.swap).toMap
  
  def partialQuery: TSQuery = {
    val lfs = literalFields.toList
    val conjuncts = for ((f, v) <- lfs) yield v.toConjunct(f)
    Conjunction(conjuncts.toList:_*)
  }
  
  def joinKeys: Map[TVariable, String] = {
    val vfs: Map[TVariable, Field] = varsToFields
    val pairs = for (v <- vfs.keys; f <- vfs.get(v); a = name + "." + f)
      yield (v, a)
    pairs.toMap
  }
  
  def attrName(v: TVariable): Option[String] = joinKeys.get(v)
  
  def vars: Iterable[TVariable] = varsToFields.keys.toSet
  
  def subs(bindings: Map[TVariable, TVal]) = {
    val newValues = values map {
      case (field, value: TVariable) if bindings.contains(value) => (field, bindings(value))
      case (field, value: TLiteral) => (field, value.subs(bindings))
      case (field, value) => (field, value)
    }
    copy(values = newValues)
  }
  
  def subs(tvar: TVariable, tval: TVal): TConjunct = subs(Map(tvar -> tval))
  
  def rename(n: String) = copy(name = n)
  
  val xs = values.getOrElse(arg1, "")
  val rs = values.getOrElse(rel, "")
  val ys = values.getOrElse(arg2, "")
  override def toString = s"($xs, $rs, $ys)"
}
case object TConjunct {
  
  val logger = LoggerFactory.getLogger(this.getClass) 
  
  val qpat = """\(?(.+),(.+),(.+?)\)?""".r

  def fromString(name: String, s: String): Option[TConjunct] = s match {
    case qpat(x, r, y) => Some(fromTriple(name, x, r, y))
    case _ => None
  }
  
  def getTLiteral(s: String): TLiteral = QuotedTLiteral.fromString(s) match {
    case Some(QuotedTLiteral(y)) => QuotedTLiteral(y)
    case _ => UnquotedTLiteral(s)
  }

  def getTVal(s: String): TVal = {
    val v = TVariable.fromString(s)
    v match {
      case Some(TVariable(x)) => TVariable(x)
      case _ => getTLiteral(s)
    }
  }
  
  val fields = List(arg1, rel, arg2)
  def fromTriple(name: String, x: String, r: String, y: String): TConjunct = {
    val lst = List(x.trim(), r.trim(), y.trim())
    val items = for ((f, a) <- fields.zip(lst); v = getTVal(a)) yield (f, v)
    TConjunct(name, items.toMap)
  }
  
  val splitPat = """(?<=\))\s*?(?=\()"""
  def fromStringMult(s: String): Iterable[TConjunct] = {
    val parts = s.split(splitPat).toList.map(_.trim).filterNot(_ == "")
    val queries = { for ((s, i) <- parts.zipWithIndex; 
                       q <- fromString(s"r$i", s)) yield q }.toList
    queries
  }
  
  def replaceField(c: TConjunct, f: Field, vs: List[TVal]): List[TConjunct] = {
    for (v <- vs) yield TConjunct(c.name, c.values + (f -> v))
  }

} 

/**
 * A conjunctive query consists of a list of qvars (query variables) and a list 
 * of conjuncts. A conjunctive query represents a select-join-project type
 * operation. The list of conjuncts represents the data to be selected.
 * The shared variables among the conjuncts encodes the join predicates. 
 * The qVars encodes the projection variables. qAttr is the tuple-attribute
 * to project onto. 
 */
trait ConjunctiveQuery {
  def qVars: List[TVariable]
  def qAttrs: List[String]
  def conjuncts: List[TConjunct]
  def subs(bindings: Map[TVariable, TVal]): ConjunctiveQuery
  def subs(tvar: TVariable, tval: TVal): ConjunctiveQuery = subs(Map(tvar -> tval))
  def combine(cq: ConjunctiveQuery): ConjunctiveQuery
  def renameConjuncts(prefix: String): ConjunctiveQuery
  
  override def toString(): String = {
    val varString = qVars.map(_.toString).mkString(",")
    val conjString = conjuncts.mkString(" ")
    varString + ": " + conjString 
  }
  
  def joinPairs = {
    val grouped = conjuncts.flatMap(_.joinKeys.toList).groupBy(_._1)
    for {
      variable <- grouped.keys
      (variable1, fieldName1) <- grouped(variable)
      (variable2, fieldName2) <- grouped(variable)
      if fieldName1 < fieldName2
    } yield (fieldName1, fieldName2)
  }
  
}

case class FieldIndex(conjunctName: String, field: Field) {
  private def updateConjunct(c: TConjunct, fn: TVal => TVal): TConjunct = {
    val newVals = for {
      (f, v) <- c.values
      newv = if (c.name == conjunctName && f == field) fn(v) else v
    } yield (f, newv)
    TConjunct(c.name, newVals)
  }
  def updateQuery(q: ConjunctiveQuery, fn: TVal => TVal): ConjunctiveQuery = {
    val newConjs = q.conjuncts.map(updateConjunct(_, fn))
    ListConjunctiveQuery(q.qVars, newConjs)
  }
}

/**
 * A conjunctive query backed by a list of conjuncts.
 */
case class ListConjunctiveQuery(qVars: List[TVariable], conjuncts: List[TConjunct])
  extends ConjunctiveQuery {
  
  private val logger = LoggerFactory.getLogger(this.getClass) 
  val conjunctNames = conjuncts.map(_.name)
  if (conjunctNames.distinct.size != conjunctNames.size) throw new 
    IllegalArgumentException(s"Conjuncts must have distinct names: $conjuncts")
  
  val qas = {for (v <- qVars; c <- conjuncts; a <- c.attrName(v)) yield (v, a)}.groupBy(_._1)
  val qAttrs = for (v <- qVars; group <- qas.get(v); (v, a) <- group.find(x => true)) yield a
  
  override def subs(bindings: Map[TVariable, TVal]) = {
    val newConjs = conjuncts.map(_.subs(bindings))
    val newQVars = newConjs.flatMap(_.vars).distinct
    ListConjunctiveQuery(newQVars, newConjs)
  }
  
  def renameConjuncts(prefix: String) = {
    val newConjs = conjuncts.zipWithIndex map {
      case (c, i) => c.rename(s"${prefix}.$i")
    }
    copy(conjuncts = newConjs)
  }
  
  override def combine(cq: ConjunctiveQuery) = {
    val conjNames1 = this.conjunctNames.toSet
    val conjNames2 = cq.conjuncts.map(_.name).toSet
    val namesIntersect = !conjNames1.intersect(conjNames2).isEmpty
    val conjs1 = if (namesIntersect) {
      logger.warn(s"Conjunction field names in ${this} and $cq intersect, renaming 'r' and 's': $conjNames1 vs. $conjNames2")
      this.renameConjuncts("r").conjuncts
    } else {
      this.conjuncts
    }
    val conjs2 = if (namesIntersect) {
      cq.renameConjuncts("s").conjuncts
    } else {
      cq.conjuncts
    }
    val newConjs = conjs1 ++ conjs2
    val newVars = (this.qVars ++ cq.qVars).distinct
    ListConjunctiveQuery(newVars, newConjs)
  }
  
}
case object ListConjunctiveQuery {
  def fromString(s: String): Option[ListConjunctiveQuery] = {
    val parts = s.split(":", 2)
    if (parts.size == 2) {
      val left = parts(0)
      val qVars = TVariable.fromStringMult(parts(0)) match {
        case head :: rest => head :: rest
        case _ => 
          throw new IllegalArgumentException(s"Expected variable: $left")
      }
      val conjuncts = TConjunct.fromStringMult(parts(1))
      Some(ListConjunctiveQuery(qVars, conjuncts.toList))
    } else if (parts.size == 1) {
      val s = parts(0)
      val conjuncts = TConjunct.fromStringMult(s)
      val qVars = conjuncts.flatMap(_.vars).toList match {
        case v :: rest => v :: rest
        case _ => throw new IllegalArgumentException(s"Expected variable: $s")
      }
      Some(ListConjunctiveQuery(qVars.distinct, conjuncts.toList))
    } else {
      None
    }
  }

}

/**
 * A simple query is a conjunctive query that has a single conjunct.
 */
case class SimpleQuery(name: String, map: Map[Field, TVal])
  extends ConjunctiveQuery { 
  val conjunct = TConjunct(name, map)
  val conjuncts = List(conjunct)
  val vars = map.values.collect{ case x: TVariable => x }.toList
  val qVars = vars match {
    case v :: Nil => List(v)
    case _ => throw new 
      IllegalArgumentException(s"SimpleQuery must have exactly one variable, "
          + s"got: $vars")
  }
  val qAttrs = List(conjunct.joinKeys(qVars(0)))
  override def subs(bindings: Map[TVariable, TVal]) = {
    val newConj = conjunct.subs(bindings)
    copy(map = newConj.values)
  }
  override def renameConjuncts(prefix: String) = {
    copy(map = conjunct.rename(s"${prefix}0").values)
  }
  override def combine(cq: ConjunctiveQuery) = {
    val newConjs = (this.conjuncts ++ cq.conjuncts).distinct
    val newVars = (this.qVars ++ cq.qVars).distinct
    ListConjunctiveQuery(newVars, newConjs)
  }
}
case object SimpleQuery {
  def fromString(s: String) = TConjunct.fromString("r", s) match {
    case Some(TConjunct(name, map)) => Some(SimpleQuery(name, map))
    case _ => None
  }
}

object Utils {
  def cartesian[A](xs: Traversable[Traversable[A]]): Seq[Seq[A]] =
    xs.foldLeft(Seq(Seq.empty[A])) {
    (x, y) => for (a <- x.view; b <- y) yield a :+ b
  }
}
