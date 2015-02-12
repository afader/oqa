package edu.knowitall.eval

import edu.knowitall.tool.tokenize.StanfordTokenizer
import edu.knowitall.tool.postag.StanfordPostagger
import edu.knowitall.tool.stem.MorphaStemmer
import java.io.InputStream
import scala.io.Source
import java.io.FileInputStream
import java.io.PrintWriter
import java.io.File
import org.slf4j.LoggerFactory

trait Oracle {
  
  def inputs: List[String]

  def getLabel(input: String, output: String): Option[Boolean]
  
  def getCorrectOutputs(input: String): List[String]
  
  def isCorrect(input: String, output: String): Boolean = {
    getLabel(input, output) match {
      case Some(label) => label
      case _ => throw new IllegalStateException(s"No label for ($input, $output)")
    }
  }
  
  def hasLabel(input: String, output: String): Boolean = {
    getLabel(input, output) match {
      case Some(label) => true
      case _ => false
    }
  }
  
  def toTrainingSet: List[(String, Set[String])] = {
    for (i <- inputs; outputs = getCorrectOutputs(i).toSet; if outputs.size > 0)
      yield (i, outputs)
  }

}

trait UpdateableOracle extends Oracle {
  def save
  def update(input: String, output: String, label: Boolean)
}

object Oracle {
    
  val tokenizer = new StanfordTokenizer()
  val tagger = new StanfordPostagger()
  val lemmatizer = new MorphaStemmer()
  
  def normalizePair(input: String, output: String) = (normalize(input), normalize(output))

  def decompose(s: String): String = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+", "").replaceAll("\\P{Print}", "");
  
  def normalize(t: String): String = {
    val s = decompose(t)
    val tokens = tokenizer(s)
    if (tokens.size > 0) {
      val tagged = tagger.postagTokenized(tokens)
      val result = tagged.map(lemmatizer.lemmatizePostaggedToken(_).lemma.toLowerCase)
      result.mkString(" ")
    } else {
      s.toLowerCase().mkString(" ")
    }
  }
  
  def readLine(line: String): (String, String, Boolean) = {
    val fields = line.split("\t", 4)
    fields match {
      case Array(tag, l, i, o) => (normalize(i), normalize(o), getBoolean(l))
      case _ => throw new IllegalArgumentException(s"Could not parse line: '$line'")
    }
  }
  def getBoolean(s: String) = s.toLowerCase() match {
    case "0" => false
    case "false" => false
    case _ => true
  }
  
  def labelsFromInputStream(is: InputStream) = {
    val triples = Source.fromInputStream(is, "UTF8").getLines.filter(_.startsWith("LABEL\t")).map(readLine)
    triples.map(triple => (normalize(triple._1), normalize(triple._2)) -> triple._3).toMap
  }
  
  def fromFile(fn: String) = labelsFromInputStream(new FileInputStream(fn))
} 

class FileOracle(path: String) extends UpdateableOracle {
  
  val logger = LoggerFactory.getLogger(this.getClass)

  
  val file = new File(path)
  val labels = if (file.exists()) {
    scala.collection.mutable.Map(Oracle.fromFile(path).toSeq: _*)
  } else {
    scala.collection.mutable.Map[(String, String), Boolean]()
  }
   
  def correctOutputs = {
    val pairs = for ((input, output) <- labels.keys; if labels.getOrElse((input, output), false)) yield (input, output)
    val grouped =  pairs.groupBy(_._1).map { case (k,v) => (k,v.map(_._2).toList)} 
    grouped.toMap
  }
  override def inputs = { 
    labels.keys.map(_._1).toList
  }
  def normalize = Oracle.normalize _
  override def getLabel(input: String, output: String) = {
    val i = normalize(input)
    val o = normalize(output)
    val attempt = labels.get((i, o))
    attempt match {
      case Some(value) => Some(value)
      case None => labels.get(normalize(i), normalize(o))
    }
  }
  override def getCorrectOutputs(input: String): List[String] = correctOutputs.getOrElse(Oracle.normalize(input), List())

  def save = {
    logger.debug(s"Saving labels to $path")
    val output = new PrintWriter(path, "UTF8")
    for (((i, o), l) <- labels) output.println(s"LABEL\t$l\t$i\t$o")
    output.close()
  }
  def update(i: String, o: String, label: Boolean) = {
    labels += ((normalize(i), normalize(o)) -> label)
  }
}