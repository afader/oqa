package edu.knowitall.paraphrasing.rules

import edu.knowitall.parsing.cg.SentencePattern
import edu.knowitall.execution.UnquotedTLiteral
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.execution.TVariable
import edu.knowitall.parsing.cg.LexiconPreprocessor
import edu.knowitall.search.qa.QaAction

case class ParaphraseRule(name: String, input: SentencePattern, output: String) extends QaAction {
  private val sp = UnquotedTLiteral(output)
  def apply(s: Sentence with Chunked with Lemmatized) = if (input.matches(s)) {
    val groups = input.groups(s)
    val bindings = groups map { 
      case (name, value) => (TVariable(name), UnquotedTLiteral(value))
    }
    val result = sp.subs(bindings)
    Some(result.value)
  } else {
    None
  }
  override def toString = name
}

case object ParaphraseRule {
  
  lazy val preprocessor = LexiconPreprocessor()
  
  def fromStrings(strings: IndexedSeq[String]) = for {
    line <- strings
    if !line.trim.startsWith("#") && line.trim != ""
  } yield fromString(line)
  
  def fromString(s: String) = s.split("\\s+:=\\s+", 2) match {
    case Array(input, output) => input.split("\\s+", 2) match {
      case Array(name, pat) => 
        ParaphraseRule(name, SentencePattern(preprocessor(pat)), output)
      case _ => throw new IllegalArgumentException(s"Invalid pattern: $input")
    }
    case _ => throw new IllegalArgumentException(s"Invalid rule: $s") 
  }
  
}