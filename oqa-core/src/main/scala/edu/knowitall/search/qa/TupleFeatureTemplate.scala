package edu.knowitall.search.qa

import edu.knowitall.execution.Tuple
import edu.knowitall.learning.SparseVector
import com.typesafe.config.ConfigFactory
import edu.knowitall.util.ResourceUtils

case class TupleFeatureTemplate(
    attr: String,
    agg: (Double, Double) => Double,
    min: Double,
    max: Double,
    scale: Double => Double) {
  
  private val smin = scale(min)
  private val smax = scale(max)
  private val suffix = s".$attr"
  
  private def computeValue(v: Double) = if (min <= v && v <= max) {
    Some((scale(v) - smin) / (smax - smin))
  } else {
    None
  }
  
  def apply(tuple: Tuple): SparseVector = {
    val svalues = for {
      a <- tuple.attrs.keys
      if a.endsWith(suffix)
      v <- tuple.getNumber(a)
      r <- computeValue(v)
    } yield r
    if (svalues.isEmpty) {
      SparseVector.zero
    } else {
      SparseVector(s"tuple $attr" -> svalues.reduce(agg))
    }
  }
  
}

object TupleFeatureTemplate {
  val conf = ConfigFactory.load()
  val res = conf.getString("tuplefeatures.resourcePath")
  lazy val defaultTemplates = ResourceUtils.resourceSource(res).getLines.map(fromString).toList
  def fromString(s: String) = {
    val fields = s.trim.split("\t").toList
    fields match {
      case attr :: sagg :: smin :: smax :: sscale :: Nil => {
        val agg = aggFromString(sagg)
        val scale = scaleFromString(sscale)
        val min = smin.toDouble
        val max = smax.toDouble
        TupleFeatureTemplate(attr, agg, min, max, scale)
      }
      case _ => throw new IllegalArgumentException(s"Invalid feature template: $s")
    }
  }
  def aggFromString(s: String): (Double, Double) => Double = s.trim.toLowerCase match {
    case "min" => Math.min _
    case "max" => Math.max _
    case _ => throw new IllegalArgumentException(s"Invalid aggregator: $s")
  }
  def scaleFromString(s: String): Double => Double = s.trim.toLowerCase match {
    case "log" => Math.log
    case "linear" => (x: Double) => x
    case _ => throw new IllegalArgumentException(s"Invalid feature scale: $s")
  }
}