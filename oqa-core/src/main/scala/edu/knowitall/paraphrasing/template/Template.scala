package edu.knowitall.paraphrasing.template

import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.util.NlpUtils.makeRegex
import scala.collection.JavaConversions._
import edu.knowitall.util.NlpUtils
import com.typesafe.config.ConfigFactory
import edu.knowitall.tool.stem.Lemmatized.viewAsToken

case class Template(left: Seq[Lemmatized[ChunkedToken]], right: Seq[Lemmatized[ChunkedToken]]) {
  def substitute(value: Seq[Lemmatized[ChunkedToken]]) = left ++ value ++ right
  def serialize = NlpUtils.serialize(left) + "|" + NlpUtils.serialize(right)
  val templateString = (left.map(_.lemma.toLowerCase()).mkString(" ") + " $y " + right.map(_.lemma.toLowerCase()).mkString(" ")).trim
}

case object Template {
  def deserialize(s: String): Template = {
    s.split("|") match {
      case Array(s1, s2) => Template(NlpUtils.deserialize(s1), NlpUtils.deserialize(s2))
      case _ => throw new IllegalArgumentException(s"Could not deserialize template: $s")
    }
  }
}

case class AbstractedQuestion(value: Seq[Lemmatized[ChunkedToken]], template: Template) {
  def this(q: Seq[Lemmatized[ChunkedToken]], i: Int, j: Int) = this(q.slice(i, j), Template(q.slice(0, i), q.slice(j, q.size)))
  def substitute = template.substitute(value)
  val valueString = value.map(_.lemma.toLowerCase()).mkString(" ").trim()
  def serialize = NlpUtils.serialize(value) + "|" + template.serialize 
}

case object AbstractedQuestion {
  
  val conf = ConfigFactory.load()
  
  def deserialize(s: String): AbstractedQuestion = {
    s.split("|", 1) match {
      case Array(value, rest) => AbstractedQuestion(NlpUtils.deserialize(value), Template.deserialize(rest))
      case _ => throw new IllegalArgumentException(s"Could not deserialize abstracted question: $s")
    }
  }
  
  val maxSize = conf.getInt("paraphrase.template.maxArgLength")
  val valuePattern = makeRegex("^<pos='$' | pos='PRP$' | pos='CD' | pos='DT' | pos='JJ' | pos='JJS' | pos='JJR' | pos='NN' " +
      "| pos='NNS' | pos='NNP' | pos='NNPS' | pos='POS' | pos='PRP' | pos='RB' | pos='RBR' | pos='RBS' " +
      "| pos='VBN' | pos='VBG'>+$")
  val argPos = "$ PRP$ CD DT JJ JJS JJR NN NNS NNP NNPS POS PRP RB RBR RBS VBN VBG".split(" ").toSet
      
  def keepArg(arg: Seq[Lemmatized[ChunkedToken]]) = arg.forall(t => argPos.contains(t.postag))
      
  def intervals(size: Int, max: Int) =
    for (i <- Range(0, size); j <- Range(i, size); if j+1-i <= max) yield (i, j+1)
    
  def detInTemplate(abs: AbstractedQuestion) = abs.template.left.size > 0 && abs.template.left.last.postag == "DT"
  
  def keepAbs(abs: AbstractedQuestion) = !detInTemplate(abs)
    
  def generateAbstracted(question: Seq[Lemmatized[ChunkedToken]]): Iterable[AbstractedQuestion] = 
    for ((i, j) <- intervals(question.size, maxSize); 
         arg = question.slice(i, j);
         if keepArg(arg);
    	 abs = new AbstractedQuestion(question, i, j);
    	 if keepAbs(abs))
    	 //if valuePattern(abs.value) && keepAbs(abs))
      yield abs
  
}