package edu.knowitall.parsing.cg

import edu.knowitall.taggers.tag.PatternTagger
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.execution.TVariable
import edu.knowitall.taggers.NamedGroupType
import edu.knowitall.tool.typer.Type
import scala.util.Try
import edu.knowitall.util.NlpTools

case class PatternExtractor(patternName: String, pattern: SentencePattern) {
  def matches(s: Sentence with Chunked with Lemmatized) = pattern.matches(s)
  def extract(s: Sentence with Chunked with Lemmatized): Map[TVariable, String] = {
    pattern.groups(s) map {
      case (name, value) => (TVariable(name) -> value) 
    }
  }
  def apply(s: Sentence with Chunked with Lemmatized) = extract(s)
}

case object PatternExtractor {
  private val defPattern = "^([A-Za-z]+[a-z0-9]*)\\s*(.*)$".r
  def fromString(s: String) = s.trim match {
    case defPattern(name, pattern) => PatternExtractor(name, SentencePattern("^"+pattern+"$"))
    case _ => throw new IllegalArgumentException("Invalid definition: $s")
  }
  def fromLines(lines: Iterable[String]) = lines map fromString 
}