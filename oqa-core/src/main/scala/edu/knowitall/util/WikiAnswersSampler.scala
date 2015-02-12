package edu.knowitall.util

import edu.knowitall.common.Resource.using
import java.io.PrintStream
import scala.Array.canBuildFrom

/**
 * A utility (throw-away?) for sampling answers.
 */
class WikiAnswersSampler(val inputFile: String) extends Iterable[Set[String]] {

  import io.Source
  
  /**
   * Get the median-length question.
   */
  def processLine(line: String): Set[String] = {
    
    val parts = line.split("\t")
    val questions = parts.filter(_.startsWith("q:"))
    val cleaned = questions.map(_.drop(2).trim)
    cleaned.toSet
  }
  
  def iterator = new Iterator[Set[String]]() {
    val source = io.Source.fromFile(inputFile, "UTF8")
    val lines = source.getLines
    val questions = lines map processLine
    var closed = false
    def hasNext = {
      if (closed) false
      else if (!questions.hasNext) {
        source.close
        closed = true
        false
      }
      else true
    }
    def next = questions.next
  }
}

object WikiAnswersSampler {
  
  import edu.knowitall.common.Resource.using
  import java.io.PrintStream
  
  def main(args: Array[String]): Unit = {

    val inputFile = args(0)
    val outputStream = if (args.length == 1) System.out else new PrintStream(args(1))

    val waSampler = new WikiAnswersSampler(inputFile)

    using(outputStream) { output =>
      waSampler foreach { qset => output.println(qset.mkString("\t")) }
    }
  }
}