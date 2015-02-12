package edu.knowitall.eval

import scala.io.Source
import scala.collection.JavaConverters._
import java.io.File
import com.typesafe.config.ConfigFactory
import java.io.PrintWriter
import com.typesafe.config.ConfigRenderOptions

abstract class SystemOutput {
  
  def inputs: List[String] = records.map(_.input).distinct
  
  def inputOutputs: List[(String, String)] = records.map(r => (r.input, r.output)).toList.distinct
  
  def config: Map[String, String]
  
  def path: String
  
  def normalize = Oracle.normalize _
  
  def recordsFor(input: String, output: String): List[OutputRecord] = {
    val in = normalize(input)
    val on = normalize(output)
    inputOutputToRecords.getOrElse((in, on), List()).toList
  }
  

  private val inputToRecords = records.groupBy(r => normalize(r.input))
  
  def recordsFor(input: String): List[OutputRecord] = inputToRecords.getOrElse(normalize(input), List())
  

  private val inputOutputToRecords = records.groupBy(r => (normalize(r.input), normalize(r.output)))
  
  def topOutputFor(input: String): Option[String] = {
    inputToRecords.get(normalize(input)) match {
      case Some(l) => Some(l.maxBy(_.score).output)
      case _ => None
    }
  }
  
  def topScoreFor(input: String, output: String): Option[Double] = {
    recordsFor(input, output) match {
      case Nil => None
      case l: List[OutputRecord] => Some(l.maxBy(_.score).score)  
    }
  }
  
  def hasOutputFor(input: String) = inputToRecords.contains(input)
  
  def records: List[OutputRecord]
  
  def save = {
    
    val dir = new File(path)
    if (dir.exists() && !dir.isDirectory()) throw new IllegalStateException(s"$dir exists but is not a directory")
    if (!dir.exists()) dir.mkdirs()
    
    val outputPath = new File(path, SystemOutput.outputFile)
    val outputWriter = new PrintWriter(outputPath)
    records.foreach(outputWriter.println(_))
    outputWriter.close()
    
    val configPath = new File(path, SystemOutput.configFile)
    val configWriter = new PrintWriter(configPath)
    for ((k, v) <- config) configWriter.println(s"$k\t$v")
    configWriter.close()
     
  }

}

case object SystemOutput {
  val conf = ConfigFactory.load()
  val outputFile = conf.getString("eval.output.file")
  val configFile = conf.getString("eval.config.file")
  val nameFile = conf.getString("eval.name.file")

  private case class SystemOutputImpl(path: String, records: List[OutputRecord], config: Map[String, String]) extends SystemOutput {
    def apply(path: String, records: List[OutputRecord], config: Map[String, String]) = SystemOutputImpl(path, records, config)    
  }
  
  def apply(path: String, records: List[OutputRecord], config: Map[String, String]): SystemOutput = new SystemOutputImpl(path, records, config)
  def apply(path: String, records: List[OutputRecord]): SystemOutput = new SystemOutputImpl(path, records, getCurrentConfig)
  
  def loadRecords(path: String): List[OutputRecord] = {
    val lines = Source.fromFile(new File(path, outputFile), "UTF8").getLines
    lines.map(OutputRecord.fromLine).toList
  }
  def loadConfig(path: String) = {
    val lines = Source.fromFile(new File(path, configFile)).getLines
    val pairs = lines map { line: String =>
      line.split("\t", 2) match {
        case Array(k, v) => (k, v)
        case _ => throw new IllegalArgumentException(s"Could not parse line: $line")
      }
    }
    pairs.toMap
  }
  def getCurrentConfig = conf.root().asScala.map(pair => (pair._1, pair._2.render(ConfigRenderOptions.concise))).toMap
  
  def fromPath(path: String): SystemOutput = SystemOutputImpl(path, loadRecords(path), getCurrentConfig)
}