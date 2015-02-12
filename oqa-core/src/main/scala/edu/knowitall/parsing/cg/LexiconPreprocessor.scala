package edu.knowitall.parsing.cg

import com.typesafe.config.ConfigFactory
import scala.io.Source
import edu.knowitall.util.ResourceUtils
import java.util.regex.Matcher

case class LexiconPreprocessor(macros: Map[String, String] = LexiconPreprocessor.defaultMacros) {
  val mlist = macros.toList
  def apply(s: String) = applyMacros(mlist, s)
  private def applyMacros(nr: List[(String, String)], s: String): String = nr match {
    case Nil => s
    case (name, replacement) :: rest => applyMacros(rest, applyMacro(name, replacement, s))
  }
  private def applyMacro(name: String, replacement: String, s: String) = {
    s.replaceAll(s"@${name}\\b", Matcher.quoteReplacement(replacement))
  }
  def update(line: String) = line.split(" ", 2) match {
    case Array(name, value) => copy(macros = macros.updated(name, "(?:" + this(value) + ")"))
    case _ => throw new IllegalArgumentException(s"Invalid macro instruction: $line")
  }
}

object LexiconPreprocessor {
  val conf = ConfigFactory.load()
  
  private def isInstr(line: String) = {
    val linet = line.trim
    linet != "" && !linet.startsWith("#") && !linet.startsWith("//")
  }
  
  def fromLines(lines: List[String], prep: LexiconPreprocessor = LexiconPreprocessor(Map.empty)): LexiconPreprocessor = lines match {
    case Nil => prep
    case line :: rest if isInstr(line) => fromLines(rest, prep.update(line)) 
    case line :: rest => fromLines(rest, prep)
  }
  
  lazy val defaultPreprocessor = if (conf.hasPath("parsing.cg.macroPath")) {
    val lines = Source.fromFile(conf.getString("parsing.cg.macroPath"), "UTF-8").getLines
    fromLines(lines.toList)
  } else if (conf.hasPath("parsing.cg.macroClasspath")) {
    val p = conf.getString("parsing.cg.macroClasspath")
    val lines = Source.fromInputStream(ResourceUtils.resource(p), "UTF-8").getLines
    fromLines(lines.toList)
  } else {
    LexiconPreprocessor(Map.empty[String, String])
  }
  lazy val defaultMacros = defaultPreprocessor.macros
}