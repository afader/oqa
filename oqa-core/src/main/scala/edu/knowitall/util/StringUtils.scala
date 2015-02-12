package edu.knowitall.util

object StringUtils {
  
  def parseDouble(s: String): Option[Double] = try { Some(s.toDouble) } catch { case e:Throwable => None }
  
  private val varPat = """\$([A-Za-z0-9_]+)""".r
  def interpolate(s: String, bindings: Map[String, String]) = {
    def acc(s: String, pairs: List[(String, String)]): String = pairs match {
      case Nil => s
      case (k, v) :: rest => acc(s.replaceAllLiterally("$"+k, v), rest) 
    }
    acc(s, bindings.toList)
  }

}