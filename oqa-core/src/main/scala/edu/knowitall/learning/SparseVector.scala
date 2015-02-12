package edu.knowitall.learning

import scala.io.Source
import java.io.PrintWriter
import scala.language.implicitConversions
import java.io.InputStream
import java.io.File
import java.io.FileInputStream
import edu.knowitall.execution.Tabulator

abstract class SparseVector {
  def activeComponents: Iterable[String]
  def apply(i: String): Double
  def add(that: SparseVector): SparseVector
  def scalarMult(x: Double): SparseVector
  def dot(that: SparseVector): Double
  def subtract(that: SparseVector): SparseVector = this.add(that.scalarMult(-1.0))
  def +(that: SparseVector): SparseVector = this.add(that)
  def -(that: SparseVector): SparseVector = this.subtract(that)
  def *(x: Double): SparseVector = this.scalarMult(x)
  def *(that: SparseVector): Double = this.dot(that)
  def /(x: Double): SparseVector = this.scalarMult(1/x)
  def activeComponents(that: SparseVector): Iterable[String] = (this.activeComponents ++ that.activeComponents).toList.distinct
  def toTable = {
    val pairs = for (c <- activeComponents.toList.sortBy(-this(_)); if Math.abs(this(c)) > 1e-9) yield Seq(c, this(c))
    Tabulator.format(Seq("Feature", "Weight") +: pairs.toSeq)
  }
}

object SparseVector {
  implicit def dPairToSparseVector(x: (String, Double)): SparseVector = SparseVectorImpl(Map(x._1 -> x._2))
  implicit def bPairToSparseVector(x: (String, Boolean)): SparseVector = SparseVectorImpl(Map(x._1 -> {if (x._2) 1.0 else 0.0}))
  implicit def iPairToSparseVector(x: (String, Int)): SparseVector = SparseVectorImpl(Map(x._1 -> x._2.toDouble))
  implicit def pairsToSparseVector(pairs: TraversableOnce[(String, Double)]): SparseVector = SparseVectorImpl(pairs.toMap)
  implicit def stringListToSparseVector(list: TraversableOnce[String]): SparseVector = SparseVectorImpl(list.map(s => (s, 1.0)).toMap)
  implicit def stringToSparseVector(s: String): SparseVector = SparseVectorImpl(Map(s -> 1.0))
  implicit def oStringToSparseVector(os: Option[String]): SparseVector = os match {
    case Some(s) => SparseVectorImpl(Map(s -> 1.0))
    case _ => SparseVectorImpl(Map())
  }
  def apply: SparseVector = SparseVectorImpl(Map())
  def apply(pairs: TraversableOnce[(String, Double)]): SparseVector = SparseVectorImpl(pairs.toMap)
  def apply(pairs: (String, Double)*): SparseVector = SparseVectorImpl(pairs.toMap)
  def fromFile(path: String): SparseVector = {
    fromInputStream(new FileInputStream(new File(path)))
  }
  def fromInputStream(in: InputStream): SparseVector = {
    val lines = Source.fromInputStream(in, "UTF8").getLines
    val pairs = lines.map { line => line.split("\t") match {
      case Array(k, v) => (k, v.toDouble)
      case _ => throw new IllegalStateException(s"Could not parse line: '$line'")
    }}.toIterable
    SparseVector(pairs)
  }
  def toFile(vector: SparseVector, path: String) = {
    val writer = new PrintWriter(path)
    vector.activeComponents.foreach(k => writer.println(s"${k}\t${vector(k)}"))
    writer.close()
  }
  private case class SparseVectorImpl(map: Map[String, Double]) extends SparseVector {
    override def activeComponents = map.keys
    override def apply(i: String) = map.getOrElse(i, 0.0)
    override def add(that: SparseVector): SparseVector = {
      val pairs = for (k <- this.activeComponents(that)) yield (k, this(k) + that(k))
      SparseVectorImpl(pairs.toMap)
    }
    override def scalarMult(x: Double): SparseVector = {
      val pairs = for((k, v) <- map) yield (k, x * v)
      SparseVectorImpl(pairs.toMap)
    }
    override def dot(that: SparseVector): Double = {
      this.activeComponents(that).map(i => this(i) * that(i)).sum
    }
    override def toString = this.map.toString.replaceFirst("Map", "SparseVector")
  }
  final val zero = SparseVector()
}