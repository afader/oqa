package edu.knowitall.util

object PPDBParser extends App {

  val source = io.Source.fromFile(args(0), "UTF8")
  
  val splitRegex = """\|\|\|""".r
  
  val syns = source.getLines.map { line =>
    val split = splitRegex.split(line).map(_.trim)
    (split(1), split(2))
  } 
  val synMap = syns.toSeq.groupBy(_._1).map(p => (p._1, p._2.map(q => q._2).distinct.sorted))
  
  val testSet = Seq("be", "invent", "like", "make", "edison", "chickpea", "garbanzo", "usa", "america", "us", "of", "the", "clinton", "bill clinton")
  
  testSet foreach { str =>
    val ss = synMap.getOrElse(str, Seq("Nil"))  
    println(str + "\t" + ss.mkString(", "))
  }
}