package edu.knowitall.learning
import scala.language.implicitConversions

abstract class FeatureFunction[T] extends Function[T, SparseVector] {
  override def apply(t: T): SparseVector
  def sum(that: FeatureFunction[T]): FeatureFunction[T]
  def +(that: FeatureFunction[T]) = this.sum(that)
}

object FeatureFunction {
  def apply[T](f: T => SparseVector): FeatureFunction[T] = FeatureFunctionImpl(f)
  implicit def functionToFeatureFunction[T](f: T => SparseVector): FeatureFunction[T] = FeatureFunction(f)
  implicit def pairToFeatureFunction[T](f: T => (String, Double)): FeatureFunction[T] = FeatureFunction((t: T) => {
    val (name, value) = f(t)
    SparseVector(name -> value)
  })
  private case class FeatureFunctionImpl[T](f: Function[T, SparseVector]) extends FeatureFunction[T] {
    override def apply(t: T) = f(t)
    override def sum(that: FeatureFunction[T]): FeatureFunction[T] = FeatureFunctionImpl((t: T) => this(t) + that(t)) 
  }
}