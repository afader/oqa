package edu.knowitall.search.qa

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence

case class AbstractedArgState(
    question: String,
    argTypes: List[String],
    processed: Sentence with Chunked with Lemmatized, 
    argInterval: Interval) extends QaState { 
  val tokens = processed.lemmatizedTokens.map(_.lemma.toLowerCase)
  override def toString() = {
    val left = tokens.slice(0, argInterval.start)
    val middle = "[" +: tokens.slice(argInterval.start, argInterval.end) :+ "]"
    val right = tokens.slice(argInterval.end, tokens.size)
    val s = (left ++ middle ++ right).mkString(" ")
    s
  } 
  def queryString = {
    val left = tokens.slice(0, argInterval.start)
    val right = tokens.slice(argInterval.end, tokens.size)
    (left ++ Seq("$y") ++ right).mkString(" ")
  }
  def arg = tokens.slice(argInterval.start, argInterval.end).mkString(" ")
  override def stateType = "AbstractedArgState"
}