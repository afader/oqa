package edu.knowitall.parsing.cg

import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.util.NlpUtils
import java.io.InputStream
import scala.io.Source
import edu.knowitall.util.ResourceUtils

case class LexicalRule(syntax: PatternExtractor, semantics: CategoryPattern) extends TerminalRule {
  val name = syntax.patternName
  override def apply(interval: Interval, sent: Sentence with Chunked with Lemmatized) = {
    val span = NlpUtils.split(sent, interval.start, interval.end)
    if (syntax.matches(span)) {
      semantics(syntax(span))
    } else {
      None
    }
  }
  override def toString = name
}

object LexicalRule {
  lazy val preprocessor = LexiconPreprocessor() 
  def fromString(s: String, preprocessor: LexiconPreprocessor = preprocessor) = {
    s.split(":=", 2) match {
      case Array(synStr, semStr) => {
        val syntax = PatternExtractor.fromString(preprocessor(synStr.trim))
        val semantics = CategoryPattern.fromString(semStr.trim)
        LexicalRule(syntax, semantics)
      }
      case _ => throw new IllegalArgumentException(s"Invalid lexical rule string: $s")
    }
  }
  def fromStrings(strings: IndexedSeq[String]) = for {
    line <- strings
    if !line.trim.startsWith("#") && line.trim != ""
  } yield LexicalRule.fromString(line.trim())
  def fromInputStream(is: InputStream) = fromStrings(Source.fromInputStream(is).getLines.toIndexedSeq)
  def fromFile(path: String) = fromStrings(Source.fromFile(path).getLines.toIndexedSeq)
  def fromResource(path: String) = fromInputStream(ResourceUtils.resource(path))
}