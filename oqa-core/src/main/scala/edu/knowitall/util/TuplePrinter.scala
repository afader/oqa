package edu.knowitall.util

import edu.knowitall.execution.Tuple

object TuplePrinter {
  
  val fieldNames = Seq("arg1", "rel", "arg2")
  
  def fieldPartNames(part: Int): Seq[String] = fieldNames.map(fn => s"r$part.$fn") 
  
  def printTuplePart(tuple: Tuple, part: Int): Option[String] = {
    val parts = fieldPartNames(part)
    tuple.get(parts.head) match {
      case Some(_) => {
        val values = parts.flatMap(tuple.get)
        val strings = values.map(_.toString)
        Some(strings.mkString(", "))
      }
      case None => None
    }
  }
  def printTuple(tuple: Tuple): String = {
    val partStrings = (0 to 3).map(part => printTuplePart(tuple, part)).takeWhile(_.isDefined).map(_.get)
    partStrings.map(s => s"($s)").mkString(" ")
  }
}