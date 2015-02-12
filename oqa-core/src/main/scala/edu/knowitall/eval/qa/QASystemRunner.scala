package edu.knowitall.eval.qa

import java.io.File
import java.io.IOException
import scala.io.Source
import org.slf4j.LoggerFactory
import edu.knowitall.model.QaModel

class QASystemRunner(qa: QaModel, path: String) {
  
  val logger = LoggerFactory.getLogger(this.getClass)

  val outputFile = new File(path)
  if (!outputFile.exists()) {
    outputFile.mkdirs()
  }
  if (outputFile.exists() && !outputFile.isDirectory()) {
    throw new IOException(s"Could not write to $path, file exists")
  }
  
  def runFile(path: String) = {
    val lines = Source.fromFile(path, "UTF8").getLines.toList
    logger.info(s"Running QA System on '$path'")
    logger.info(s"'$path' contains ${lines.size} questions")
    run(path, lines)
  }
  
  def run(name: String, questions: List[String]) = {
    val n = questions.size
    val records = for ((q, i) <- questions.zipWithIndex;
               deriv <- {
                 logger.info(s"Question ${i+1} of ${n}")
                 qa.candidatePredictions(q)
               };
               r = QAOutputRecord(q, deriv.answer, deriv.score, deriv.toString))
            yield r
    val output = new QASystemOutput(path, records.toList, name)
    output.save
  }
  
}

object QASystemRunner extends App {
  val input = args(0)
  val output = args(1)
  val model = new QaModel()
  val runner = new QASystemRunner(model, output)
  runner.runFile(input)
}