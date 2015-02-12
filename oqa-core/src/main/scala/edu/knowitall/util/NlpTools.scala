package edu.knowitall.util

import edu.knowitall.tool.postag.StanfordPostagger
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.repr.sentence.Lemmatized
import edu.knowitall.repr.sentence.Chunked
import edu.knowitall.repr.sentence.Sentence
import edu.knowitall.repr.sentence.{Lemmatizer => RepLemmatizer}
import edu.knowitall.repr.sentence.{Chunker => RepChunker}
import edu.knowitall.tool.tokenize.PTBTokenizer
import edu.knowitall.tool.chunk.Chunker
import edu.knowitall.repr.sentence.Lemmatizer
import edu.knowitall.tool.stem.Stemmer

object NlpTools {
  lazy val tagger = new StanfordPostagger
  lazy val stemmer = new MorphaStemmer
  lazy val tokenizer = new PTBTokenizer
  lazy val chunker = new OpenNlpChunker
  lazy val dummyChunker = DummyChunker(tagger)
  
  def process(sentence: String, ch: Chunker = dummyChunker, lm: Stemmer = stemmer): Sentence with Chunked with Lemmatized = {
    new Sentence(sentence) with RepChunker with RepLemmatizer {
      val chunker = ch
      val lemmatizer = lm
    }
  }
  
  def normalize(text: String, ch: Chunker = dummyChunker, lm: Stemmer = stemmer) = process(text, ch, lm).lemmatizedTokens.map(_.lemma.toLowerCase).mkString(" ")
  
}