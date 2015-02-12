package edu.knowitall.execution

import scala.language.implicitConversions
import com.rockymadden.stringmetric.similarity._
import org.apache.solr.client.solrj.util.ClientUtils
import scala.Array.canBuildFrom
import scala.Option.option2Iterable

/**
 * Used to represent a Tuple (i.e. a row in a relational database). A tuple
 * has a set of String attributes. Each attribute has a value of type Any.
 */
case class Tuple(attrs: Map[String, Any]) {

  /* Concatenates two tuples together. Their attributes must be disjoint. */
  def join(other: Tuple): Tuple = {
    val t = Tuple(this.attrs ++ other.attrs)
    if (t.attrs.size == this.attrs.size + other.attrs.size) {
      return t
    } else {
      throw new
        IllegalArgumentException(s"attr names not disjoint: $this, $other")
    }
  }

  def get(a: String) = attrs.get(a)

  /* Gets the string value of attribute a. Returns None if not possible */
  def getString(a: String) = attrs.get(a) match {
    case Some(x: String) => Some(x)
    case _ => None
  }

  def getBoolean(a: String) = attrs.get(a) match {
    case Some(x: Boolean) => Some(x)
    case _ => None
  }

  def getFloat(a: String): Option[Float] = attrs.get(a) match {
    case Some(x: Float) => Some(x)
    case _ => None
  }
  
  def getInt(a: String): Option[Int] = attrs.get(a) match {
    case Some(i: Int) => Some(i)
    case _ => None
  }
  
  def getNumber(a: String): Option[Double] = attrs.get(a) match {
    case Some(i: Int) => Some(i)
    case Some(f: Float) => Some(f)
    case Some(d: Double) => Some(d)
    case _ => None
  }

  /* Renames the attributes using the given function */
  def rename(f: String => String): Tuple = {
    Tuple(attrs.map{ case (k, v) => (f(k), v) })
  }

  /* Adds "$p." before each attribute. */
  def renamePrefix(p: String): Tuple = rename(k => p + "." + k)

  override def toString: String = {
    val pairs = attrs map { case (k, v) => k + ": " + v }
    return "(" + pairs.mkString(", ") + ")"
  }
  
}

/**
 * Contains objects for select and join conditions.
 */
object Conditions {

  // Mnemonics
  type Attr = String
  type Value = Any
  type ValuePred = Value => Boolean
  type BinaryStringPred = (String, String) => Boolean
  type BinaryValuePred = (Value, Value) => Boolean
  type TuplePred = Tuple => Boolean

  def valPair(a1: Attr, a2: Attr, t: Tuple) =
    for (v1 <- t.attrs.get(a1); v2 <- t.attrs.get(a2)) yield (v1, v2)

  /* Converts the given predicate over value pairs to a tuple predicate. This
   * can be used to convert a predicate like "string1 and string2 are equal"
   * to a tuple predicate like "tuple.a1 and tuple.a2 are equal"
   */
  def binaryAttrPred(a1: Attr, a2: Attr, f: BinaryValuePred): TuplePred =
    (t: Tuple) => valPair(a1, a2, t).exists(f.tupled)


  /* Converts the given predicate over value pairs to a tuple predicate. This
   * can be used to convert a predicate like "string1 and string2 are equal"
   * to a tuple predicate like "tuple.a = x".
   */
  def unaryAttrPred(a: Attr, x: String, f: BinaryValuePred): TuplePred =
    (t: Tuple) => {
      for (v <- t.attrs.get(a)) yield (v, List(x))
    }.exists(f.tupled)

  /* Converts a predicate over (String, String) to a predicate over (Any, Any)
   * by making non-String input always return false.
   */
  def stringToAnyPred(f: BinaryStringPred): BinaryValuePred =
    (v1: Value, v2: Value) => (v1, v2) match {
      case (v1: String, v2: String) => f(v1, v2)
      case _ => false
    }

  /* A shortcut for combining stringToAnyPred and binaryAttrPred. */
  def binaryPredFromString(a1: Attr, a2: Attr, f: BinaryStringPred) =
    binaryAttrPred(a1, a2, stringToAnyPred(f))

  /* A shortcut for combining stringToAnyPred and unaryAttrPred. */
  def unaryPredFromString(a: Attr, v: String, f: BinaryStringPred) =
    unaryAttrPred(a, v, stringToAnyPred(f))

  /* Wrapper for string equality, with optional case sensitivity. */
  def StringEquality(caseSensitive: Boolean = true) = (x: String, y: String) =>
    if (caseSensitive) x == y else x.toLowerCase() == y.toLowerCase()

  /* Thresholded string-similarity predicate, returns true if the given strings
   * have similarity above a given threshold.
   */
  def strSim(thresh: Double) = (x: String, y: String) =>
    StrSim.sim(x, y) > thresh
  val strEq = StringEquality(false)
  val valEq = (x: Value, y: Value) => x == y

  /* A BinaryPred is a TuplePred that has access to two attribute names. */
  trait BinaryPred {
    def attr1: Attr
    def attr2: Attr
    def apply(t: Tuple): Boolean
  }

  /* AttrsEqual is a TuplePred that returns true if t.attr1 equals t.attr2. */
  case class AttrsEqual(attr1: Attr, attr2: Attr) extends TuplePred
    with BinaryPred {
    val pred = binaryPredFromString(attr1, attr2, strEq)
    def apply(t: Tuple) = pred(t)
  }

  /* AttrEquals is a TuplePred that returns true if t.attr equals value. */
  case class AttrEquals(attr: Attr, value: String) extends TuplePred {
    val pred = unaryPredFromString(attr, value, strEq)
    def apply(t: Tuple) = pred(t)
  }

  /* AttrsSim is a TuplePred that returns true if t.attr1 and t.attr2 have
   * similarity greater than thresh.
   */
  case class AttrsSim(attr1: Attr, attr2: Attr, thresh: Double)
  extends TuplePred with BinaryPred {
    val pred = binaryPredFromString(attr1, attr2, strSim(thresh))
    def apply(t: Tuple) = pred(t)
  }

  /* AttrSim is a TuplePred that returns true if t.attr and value have
   * similarity greater than thresh.
   */
  case class AttrSim(attr: Attr, value: String, thresh: Double)
  extends TuplePred {
    val pred = unaryPredFromString(attr, value, strSim(thresh))
    def apply(t: Tuple) = pred(t)
  }

  /* On returns a function Tuple => Tuple that projects a tuple t onto the
   * given attributes.
   */
  def On(attrs: Attr*) = (t: Tuple) => {
    val items = for (a <- attrs; v <- t.attrs.get(a)) yield (a, v)
    Tuple(items.toMap)
  }

}

/**
 * The Operators object defines relational algebra operators, which are used
 * to join, select, and project Tuple objects.
 */
object Operators {

  // Mnemonics
  type TuplePred = Tuple => Boolean
  type TupleMap = Tuple => Tuple
  type Tuples = Iterable[Tuple]

  /* Select returns only those tuples that satisfy the predicate p. */
  def Select(p: TuplePred) = (ts: Tuples) => ts.filter(p)

  /* Project transforms each tuple using the given map m. */
  def Project(m: TupleMap) = (ts: Tuples) => ts.map(m)

  /* Union groups multiple iterables of tuples into a single iterable. */
  def Union(tss: Tuples*) = tss.flatten

  /* Join takes the cartesian product of two tuples and returns only those
   * tuples that satisfy the predicate p. NestedLoopJoin implements this
   * as a nested loop over each iterable of tuples.
   */
  def NestedLoopJoin(p: TuplePred) = (ts1: Tuples, ts2: Tuples) => {
    for (t1 <- ts1.par; t2 <- ts2; j = t1.join(t2); if p(j)) yield j
  }.toList

  /* Joins together all pairs of tuples. */
  def Product(ts1: Tuples, ts2: Tuples): Tuples =
    for (t1 <- ts1; t2 <- ts2) yield t1.join(t2)

}

/**
 * Objects used to construct queries against a triplestore.
 */
object Search {

  // Mnemonics
  type Tuples = Iterable[Tuple]
  type Attr = String
  type Search = TSQuery => Tuples

  import Conditions._

  /* These are the possible fields in a triplestore. */
  /*object Field extends Enumeration {
    type Field = Value
    val arg1, rel, arg2, namespace = Value
    val arg1_exact, rel_exact, arg2_exact = Value
    // Maps some fields to their exact-match versions
    val exactMap = Map(arg1 -> arg1_exact, rel -> rel_exact, arg2 -> arg2_exact)
  }
  import Field._*/
  trait Field {
    val name: String
    def toExact: Field
  }
  object Field {
    implicit def field2string(f: Field) = f.name
  }
  val exactPat = ".*_exact$".r
  case class TSField(name: String) extends Field {
    override def toString = name
    override def toExact = name match {
      case exactPat(name) => TSField(name)
      case _ => TSField(name + "_exact")
    }
  }
  val arg1 = TSField("arg1")
  val rel = TSField("rel")
  val arg2 = TSField("arg2")
  val namespace = TSField("namespace")

  /* Used to represent a triplestore query. The only requirement is that it
   * should have some method that converts it to a Lucene query string.
   */
  trait TSQuery {
    def toQueryString: String
  }

  /* Used to escape characters that have special meanings in Lucene. */
  def luceneEscape = ClientUtils.escapeQueryChars _
  def quoteLogic(w: String) = w match {
    case "AND" => "\"AND\""
    case "OR" => "\"OR\""
    case "NOT" => "\"NOT\""
    case _ => w
  }
  def escape(w: String) = quoteLogic(luceneEscape(w))

  /* A query that searches the given field for the given keywords. Splits the
   * string v into words, and then converts them into a Lucene query
   * equal to the conjunctions of all the words. For example, if f = arg1
   * and v = "barack obama", the resulting Lucene query string will be
   * "arg1:barack AND arg1:obama".
   */
  case class FieldKeywords(f: Field, v: String) extends TSQuery {
    def toQueryString = {
      for (w <- v.trim().split("\\s+");
           x = f.toString() + ":" + escape(w))
      yield x }.mkString(" AND ")
  }

  /* A query that searches the given field for the given phrase. Uses the
   * exact-match version of the given field. For example, if f = arg1
   * and v = "barack obama", then the resulting Lucene query string will be
   * arg1_exact:"barack obama".
   */
  case class FieldPhrase(f: Field, v: String) extends TSQuery {
    val realField = f.toExact
    def toQueryString = realField.toString() + ":\"" + escape(v) + "\""
  }

  case class CountQuery(arg: String) extends TSQuery {
    def toQueryString = {
      arg match {
        case "" => "*:*" // much faster than arg1_exact:*
        // just do arg1 for speed...
        case _ => "arg1:\"%s\"".format(arg, arg)
      }
    }
  }

  /* Some shortcut functions for each of the fields. */
  val Arg1Eq = (v: String) => FieldPhrase(arg1.toExact, v)
  val Arg2Eq = (v: String) => FieldPhrase(arg2.toExact, v)
  val RelEq = (v: String) => FieldPhrase(rel.toExact, v)
  val Arg1Cont = (v: String) => FieldKeywords(arg1, v)
  val Arg2Cont = (v: String) => FieldKeywords(arg2, v)
  val RelCont = (v: String) => FieldKeywords(rel, v)
  val NamespaceEq = (v: String) => FieldKeywords(namespace, v)

  /* Returns the conjunction of the given queries. */
  case class Conjunction(conjuncts: TSQuery*) extends TSQuery {
    def toQueryString =
      conjuncts.map("(" + _.toQueryString + ")").mkString(" AND ")
  }

  /* Returns the disjunction of the given queries. */
  case class Disjunction(disjuncts: TSQuery*) extends TSQuery {
    def toQueryString =
      disjuncts.map("(" + _.toQueryString + ")").mkString(" OR ")
  }

  /* A shortcut method that adds FieldPhrase(f, v) as a conjunct to q. */
  def AndPhrase(q: TSQuery, f: Field, v: String) =
    Conjunction(q, FieldPhrase(f, v))

  /* A pattern to match the required triplestore attribute names in a tuple. */
  val tripColPat = ".*\\.(arg1|arg2|rel|namespace)$"

  /* A projection operator that maps a tuple to just the required triplestore
   * fields (arg1, rel, arg2, namespace).
   */
  def OnTripleCols(t: Tuple): Tuple =
    Tuple(t.attrs.filterKeys(a => a.matches(tripColPat)))
  def ProjectTriples(ts: Tuples) = Operators.Project(OnTripleCols)(ts)

  /* The code below is used for executing partial searches. A partial search
   * is a way to lazily represent a query's tuples without actually executing
   * the query against the triplestore. Parital searches are useful for
   * joining a small table T1 with a large table T2. Instead of loading both
   * tables into memory (which may be prohibitively slow), a partial-search
   * joiner loads T1 into memory, then executes a query for each tuple t in T1.
   * Each query is specifically searching for tuples that may be joined with t.
   * This allows the system to make many small, restricted queries, instead of
   * one large, unrestricted one.
   */

  /* This class is just a name for a (query, search) pair, where search
   * is some function that maps queries onto tuples.
   */
  case class PartialSearcher(query: TSQuery, search: Search)

  /* These patterns are used to infer the Field object to join from the string
   * attribute name in a tuple.
   */
  val Arg1Pat = "(.*)\\.arg1$".r
  val Arg2Pat = "(.*)\\.arg2$".r
  val RelPat = "(.*)\\.rel$".r

  /* This defines a partial search join algorithm. The join condition cond is
   * used both as a predicate (i.e. evaluating whether a joined tuple should
   * be kept or discarded) but also for creating the tuple-specific queries
   * on the fly.
   *
   * PartialSearchJoin is actually a function that takes a join condition
   * as input, and returns a function (Tuples, PartialSearcher) => Tuples.
   * This resulting function takes the smaller table as input, and joins it
   * using the given PartialSearcher object, which encodes the query for
   * the larger table.
   */
  def PartialSearchJoin(cond: BinaryPred) = {
    // The join attribute of the smaller table.
    val lAttr = cond.attr1

    // The join attribute of the larger table.
    val rAttr = cond.attr2

    // Code for inferring which field to search for when creating a query. It
    // is possible that rAttr might not be mappable to a field, in which case
    // it is not actually possible to execute the partial search join algo.
    val (name, field) = rAttr match {
      case Arg1Pat(n) => (n, arg1)
      case Arg2Pat(n) => (n, arg2)
      case RelPat(n) => (n, rel)
      case _ => throw new
        IllegalArgumentException(s"field must be arg1, rel, or arg2: $rAttr")
    }

    val tripleField = """r\d+\.(arg1|arg2|rel)""".r.pattern

    (ts: Tuples, ps: PartialSearcher) => {

      // split up into chunks
      val splitGroups = ts.grouped(10).map(_.toSeq)
      // make pairs of (join attributes, tuples) where join attributes is not empty.
      val attrsGroups = splitGroups.map { tuples => (tuples.flatMap(_.getString(lAttr)), tuples) } filter(_._1.nonEmpty)
      // map to new pairs of (Join Attribute Disjunction, Tuples) by converting join attributes to queries.
      val queryGroups = attrsGroups.map { case (attrs, tuples) => (Disjunction(attrs.map(a => FieldKeywords(field, a)): _*), tuples) }
      // map to (disjunctive search-join query, Tuples to join with) by combining disjunction with
      val disjunctionGroups = queryGroups.map { case (disj, tuples) => (Conjunction(disj, ps.query), tuples) }
      // for each pair, execute the disjunction and join the result with tuples
      disjunctionGroups.toSeq.par.flatMap { case (q, tuples) =>
        val qts = ps.search(q)
        for (
            t1 <- qts;
            t2 <- tuples;
            t3 = t1.join(t2);
            if (cond(t3))) yield t3
      }
    }.toList
  }
}

/**
 * This object is used to create SQL-style table printouts from tuples. The
 * code is ripped from this stackoverflow post:
 * http://stackoverflow.com/questions/7539831/scala-draw-table-to-console
 */
object Tabulator {

  def trim(s: String, l: Int) = {
    val n = s.size
    s.substring(0, Math.min(l, n))
  }

  def valToString(v: Any) = v match {
    case x @ (_ :: _ :: _) => "{" + trim(x.mkString(", "), 40) + "}"
    case x @ (y :: _) => y.toString
    case x => x.toString
  }
  def tupleToList(t: Tuple, attrs: List[String]) = {
    for(
    		a <- attrs;
    		v = t.attrs.getOrElse(a, "");
    		s = valToString(v)) yield s}.toList

  def tuplesToTable(cols: List[String], ts: Iterable[Tuple]) = {
    val lst = ts.toList
    format(cols :: lst.map(t => tupleToList(t, cols)))
  }
  
  private val namePat = "^(.*)\\.(arg1|arg2|rel|namespace)$".r
  def triplesToTable(ts: List[Tuple]): String = ts match {
    case t :: rest => {
      val conjNames = for {
        a <- t.attrs.keys
        (conj, field) <- a match {
          case namePat(conj, field) => Some((conj, field))
          case _ => None
        }
      } yield conj
      val columns = conjNames.toList.distinct.flatMap(n => List(s"${n}.arg1", s"${n}.rel", s"${n}.arg2", s"${n}.namespace"))
      tuplesToTable(columns, ts)
    }
    case _ => ""
  }

  def tuplesToTable(ts: Iterable[Tuple]): String = {
    val lst = ts.toList
    if (lst.size > 0) {
      val cols = lst(0).attrs.keys.toList
      tuplesToTable(cols, ts)
    } else {
      ""
    }
  }

  def format(table: Seq[Seq[Any]]) = table match {
    case Seq() => ""
    case _ =>
      val sizes = for (row <- table) yield (for (cell <- row)
        yield if (cell == null) 0 else cell.toString.length)
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows = for (row <- table) yield formatRow(row, colSizes)
      formatRows(rowSeparator(colSizes), rows)
  }

  def formatRows(rowSeparator: String, rows: Seq[String]): String = (
    rowSeparator ::
    rows.head ::
    rowSeparator ::
    rows.tail.toList :::
    rowSeparator ::
    List()).mkString("\n")

  def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
    val cells = (for ((item, size) <- row.zip(colSizes))
      yield if (size == 0) "" else ("%" + size + "s").format(item))
    cells.mkString("|", "|", "|")
  }

  def rowSeparator(colSizes: Seq[Int]) =
    colSizes map { "-" * _ } mkString ("+", "+", "+")
}

/**
 * A custom string-similarity object. Measures the similarity between
 * two strings. Lowercases them and removes some stop words first.
 */
object StrSim {

  import edu.knowitall.tool.stem.Lemmatized
  import edu.knowitall.tool.stem.MorphaStemmer
  import edu.knowitall.tool.postag.PostaggedToken
  import edu.knowitall.tool.chunk.OpenNlpChunker
  import edu.knowitall.tool.chunk.ChunkedToken

  val stops = Set("a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with", "i", "me", "your",
      "our", "ours", "him", "he", "his", "her", "its", "you", "that",
      "every", "all", "each", "those", "other", "both", "neither", "some",
      "'s")

  val morpha = new MorphaStemmer()

  val chunker = new OpenNlpChunker()

  def lemmatize[T <: PostaggedToken](tokens: Iterable[T]): Seq[Lemmatized[T]] =
    morpha.synchronized {
    tokens.toSeq map morpha.lemmatizePostaggedToken
  }

  def lemmatize(string: String): Seq[Lemmatized[ChunkedToken]] = chunker.synchronized(lemmatize(chunker(string)))

  def normTokens(x: String) = {
    val lc = x.toLowerCase()
    val split = lc.split("\\s+")
    val noStops = split.filter { t =>
      val lookup = !stops.contains(t)
      lookup
    }
    noStops
  }

  def norm(x: String) = normTokens(x).mkString(" ")

  def sim(x: String, y: String): Double = {
    // hack -- compare forwards and backwards to avoid high scores for prefixes
    val nx = norm(x)
    val ny = norm(y)
    val forward  = JaroWinklerMetric.compare(nx, ny).getOrElse(0.0)
    val backward = JaroWinklerMetric.compare(nx.reverse, ny.reverse).getOrElse(0.0)
    (forward + backward) / 2.0
  }
}
