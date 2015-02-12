package edu.knowitall.learning

import edu.knowitall.execution.ConjunctiveQuery
import edu.knowitall.execution.Tuple
import edu.knowitall.tool.stem.MorphaStemmer
import scala.Option.option2Iterable
import edu.knowitall.execution.TLiteral
import edu.knowitall.execution.Search

object QueryTupleSimilarity {
  
  def normalize(ss: List[String]): List[String] = ss.map(_.toLowerCase()).map(MorphaStemmer.stem)
  
  def tokenize(s: String) = s.split(" ").toList
  
  def queryWords(q: ConjunctiveQuery): List[String] = {
    val literalFields = for (c <- q.conjuncts; (field, literal) <- c.literalFields) yield literal.value
    normalize(literalFields.flatMap(tokenize))
  }
  
  def tupleWords(q: ConjunctiveQuery, t: Tuple): List[String] = {
    val values = for (c <- q.conjuncts;
    				  (field, literal) <- c.literalFields;
    				  value <- t.getString(s"${c.name}.${field}"))
    				yield value
    normalize(values.flatMap(tokenize))
  }
  
  def jaccard(x: List[String], y: List[String]): Double = {
    val xset = x.toSet
    val yset = y.toSet
    if (x.size > 0 || y.size > 0) {
      xset.intersect(yset).size.toDouble / xset.union(yset).size
    } else {
      0.0
    }
  }
  
  def similarity(q: ConjunctiveQuery, t: Tuple): Double = {
    val qws = queryWords(q)
    val tws = tupleWords(q, t)
    jaccard(qws, tws)
  }
  
  def tupleFieldWords(q: ConjunctiveQuery, t: Tuple, fields: Set[Search.Field]) = {
    val values = for {
      c <- q.conjuncts
      (field, literal) <- c.literalFields
      if fields.contains(field)
      value <- t.getString(s"${c.name}.${field}")
    } yield value
    normalize(values.flatMap(tokenize))
  }
  
  def tupleWordsFields(t: Tuple, fields: Set[Search.Field]) = {
    val values = for {
      (a, v) <- t.attrs collect {
        case (a: String, v: String) => (a, v)
      }
      f <- fields
      if a.endsWith("." + f.name)
    } yield v
    normalize(values.toList.distinct.flatMap(tokenize))
  }
  
  def queryFieldWords(q: ConjunctiveQuery, fields: Set[Search.Field]) = {
    val values = for {
      c <- q.conjuncts
      (field, literal) <- c.literalFields
      if fields.contains(field)
      value = literal.value
    } yield value
    normalize(values.flatMap(tokenize))
  }
  
  def argSimilarity(q: ConjunctiveQuery, t: Tuple) = {
    val qws = queryFieldWords(q, Set(Search.arg1, Search.arg2))
    val tws = tupleFieldWords(q, t, Set(Search.arg1, Search.arg2))
    jaccard(qws, tws)
  }
  
  def relSimilarity(q: ConjunctiveQuery, t: Tuple) = {
    val qws = queryFieldWords(q, Set(Search.rel))
    val tws = tupleFieldWords(q, t, Set(Search.rel))
    jaccard(qws, tws)
  }

  def questionQuerySimilarity(query: ConjunctiveQuery, ques: String) = {
    val queryws = queryWords(query)
    val quesws = normalize(tokenize(ques))
    jaccard(queryws, quesws)
  }
  
  def questionTupleSimilarity(ques: String, t: Tuple) = {
    val quesws = normalize(tokenize(ques))
    val twords = tupleWordsFields(t, Set(Search.arg1, Search.rel, Search.arg2))
    jaccard(quesws, twords)
  }


}