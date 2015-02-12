package edu.knowitall.parsing.cg

import edu.knowitall.taggers.pattern.PatternBuilder
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.taggers.pattern.TypedToken
import edu.washington.cs.knowitall.regex.Expression.NamedGroup
import edu.knowitall.tool.tokenize.Tokenizer

case class SentencePattern(patternString: String) {
  private val pattern = PatternBuilder.compile(patternString)
  private def buildSeq(s: Sentence with Chunked with Lemmatized) = 
    s.lemmatizedTokens.zipWithIndex map { 
      case (t, i) => TypedToken(t, i, Set.empty) 
    }
  def matches(s: Sentence with Chunked with Lemmatized) = pattern.matches(buildSeq(s))
  def groups(s: Sentence with Chunked with Lemmatized) = {
    val seq = buildSeq(s)
    val result = pattern.find(seq) match {
      case None => Map.empty
      case Some(matchObj) => for {
        i <- 0 until matchObj.groups.size
        group = matchObj.groups(i)
        tokens = s.lemmatizedTokens.slice(group.interval.start, group.interval.end).map(_.token)
        if !tokens.isEmpty
        text = Tokenizer.originalText(tokens, tokens.head.offsets.start)
        name <- group.expr match {
          case namedGroup: NamedGroup[_] => Some(namedGroup.name)
          case _ => None
        }
      } yield (name, text)
    }
    result.toMap
  }
}