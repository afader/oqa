package edu.knowitall.util
import scala.collection.mutable

case class Counter(counts: mutable.Map[String, Double]) {
  def this() = this(mutable.Map.empty[String, Double])
  def increment(k: String) = set(k, get(k) + 1.0)
  def get(k: String) = counts.getOrElse(k, 0.0)
  def apply(k: String) = get(k)
  def set(k: String, v: Double) = counts.update(k, v)
  override def toString = (counts.map { case (k, v) => s"$k\t$v" }).mkString("\n")
}