package edu.knowitall.util

import scala.collection.JavaConverters._
import edu.knowitall.common.Resource.using

object AggregateSortedCounts {
  
  val tabRegex = "\\t".r
  
  def main(args: Array[String]): Unit = {
    
    val inputFile = args(0)
    val outputFile = args(1)
    val inputSource = io.Source.fromFile(inputFile, "UTF8")
    val output = new java.io.PrintStream(outputFile, "UTF8")

    var lastFields = Option.empty[(String, String)]
    var runningCount = 0

    val outputLines = inputSource.getLines.flatMap { line =>
      tabRegex.split(line) match {
        case Array(w1, w2, count) => {
          if (Some((w1, w2)).equals(lastFields)) {
            runningCount += count.toInt
            None
          } else {

            val result = lastFields match {
              case Some((w1p, w2p)) => {
                Some(s"$w1p\t$w2p\t$runningCount")
              }
              case None => {
                None
              }
            }
            runningCount = count.toInt
            lastFields = Some((w1, w2))
            result
          }
        }
      }
    } ++ lastFields.map { case (w1, w2) => s"$w1\t$w2\t$runningCount" }
    outputLines.zipWithIndex foreach {case (line, index) =>
      if (index % 10000 == 0) System.err.println(index)
      output.println(line)
    }
    output.close()
  }
}

object AlignedWordProcessor {

  private val wsRegex = "\\s+".r
  private val walignRegex = "(\\d+)-(\\d+)".r
  
  def getAlignedPhrases(q1: String, q2: String, alignment: String): Seq[Set[String]] = {
    
    val q1Tokens = wsRegex.split(q1)
    val q2Tokens = wsRegex.split(q2)
    val wAlignments = wsRegex.split(alignment).map { case walignRegex(n1, n2) => (n1.toInt, n2.toInt) }
    val q1q2Phrases = wAlignments.groupBy(_._1).map(p=>(p._1, p._2.map(_._2))).toSeq
    val q2q1Phrases = wAlignments.map(_.swap).groupBy(_._1).map(p=>(p._1, p._2.map(_._2))).toSeq
    
    val q1TokenIndexMap = q1Tokens.zipWithIndex.map(_.swap).toMap
    val q2TokenIndexMap = q2Tokens.zipWithIndex.map(_.swap).toMap
    
    //val finalAlignments = wAlignments.map  { case (q1i, q2i) => Set(q1TokenIndexMap(q1i), q2TokenIndexMap(q2i)) }
    //finalAlignments.distinct.filter(_.size == 2)
    
    val q1q2Alignments = q1q2Phrases.map { case (q1, q2s) => Set(q1TokenIndexMap(q1), q2s.map(q2TokenIndexMap.apply).mkString(" ")) }
    val q2q1Alignments = q2q1Phrases.map { case (q2, q1s) => Set(q2TokenIndexMap(q2), q1s.map(q1TokenIndexMap.apply).mkString(" ")) }
    (q1q2Alignments ++ q2q1Alignments).distinct.filter(_.size == 2)
  }
  
  def main(args: Array[String]): Unit = {

    val inputFile = "/scratch/usr/rbart/paralex/wikianswers-paraphrases-1.0/word_alignments.txt"
    
    val output = new java.io.PrintStream(args(0))
      
    val tabRegex = "\\t".r
      
    using(io.Source.fromFile(inputFile, "UTF8")) { source =>
      val alignedWords = source.getLines.flatMap { line =>
        tabRegex.split(line) match {
          case Array(q1, q2, wa) => getAlignedPhrases(q1, q2, wa)
          case _ => {
            System.err.println(s"Warning, unparseable word alignment line: $line")
            Seq.empty
          }
        }
      }
      val cacheCountedAlignedWords = new CacheCountingIterator(1000000, alignedWords)
      cacheCountedAlignedWords.zipWithIndex foreach { case ((wSet, count), index) =>
        if (index % 10000 == 0) System.err.print(".")
        val fields = wSet.toSeq.sorted :+ count.toString
        output.println(fields.mkString("\t"))
      }
    } 
    
    output.close()
  }
}


/**
 * Takes an iterator of T, and keeps an LRU cache of the most recently accessed _cacheSize_ elements, counting
 * the number of times they were seen. When an element is evicted from the cache, it is output to the
 * iterator with its count. 
 * 
 * Maybe useful in reducing output size when you're trying to count occurrences but want to avoid
 * sorting an enormous list of completely distinct elements.
 */
class CacheCountingIterator[T](val cacheSize: Int, val source: Iterator[T]) extends Iterator[(T, Int)] {
  
  import java.util.LinkedHashMap
  
  case class MutableInt(var value: Int) { 
    def increment = { value += 1 }
  }
  
  val countCache = new LinkedHashMap[T, MutableInt](cacheSize * 2, 0.75f, true) {
    override def removeEldestEntry(entry: java.util.Map.Entry[T, MutableInt]): Boolean = {
      if (this.size() > cacheSize) {
        nextEvict = Some((entry.getKey, entry.getValue.value))
        true
      } else false
    } 
  }
  
  var nextEvict = Option.empty[(T, Int)]
  
  var finalIterator = Option.empty[Iterator[(T, Int)]]
  
  def hasNext = source.hasNext || finalIterator.exists(_.hasNext)
  
  def next(): (T, Int) = {
    // if source has more elements, try to iterate over them and wait for nextEvict to become available
    while (source.hasNext && nextEvict.isEmpty) {
      val nextT = source.next()
      countCache.asScala.getOrElseUpdate(nextT, MutableInt(0)).increment
    }
    if (finalIterator.isEmpty && !source.hasNext) {
      finalIterator = Some(countCache.asScala.iterator.map(p => (p._1, p._2.value)))
    }
    if (nextEvict.nonEmpty) {
      val nextTC = nextEvict.get
      nextEvict = None
      return nextTC
    } 
    else return finalIterator.get.next
  }
}