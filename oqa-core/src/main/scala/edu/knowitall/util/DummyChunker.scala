package edu.knowitall.util

import edu.knowitall.tool.postag.Postagger
import edu.knowitall.tool.chunk.Chunker
import edu.knowitall.tool.postag.PostaggedToken
import edu.knowitall.tool.chunk.ChunkedToken

case class DummyChunker(override val postagger: Postagger) extends Chunker {
  override def chunkPostagged(tokens: Seq[PostaggedToken]) = {
    tokens map { t => 
      new ChunkedToken(DummyChunker.sym, t.postagSymbol, t.string, t.offset)
    }
  }
}

case object DummyChunker {
  val sym = Symbol("")
}