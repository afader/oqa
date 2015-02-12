package edu.knowitall.util

import org.slf4j.LoggerFactory
import edu.knowitall.collection.immutable.Interval

object MathUtils {
  val logger = LoggerFactory.getLogger(this.getClass)

  def clip(x: Double, min: Double, max: Double) = Math.min(Math.max(x, min), max)
  def scale(x: Double, min: Double, max: Double) = (x - min) / (max - min)
  def clipScale(x: Double, min: Double, max: Double) = scale(clip(x, min, max), min, max)
  
  
  def intervals(length: Int, size: Int) = for {
    i <- 0 until (size - length + 1)
  } yield Interval.open(i, i + length)
    
  def allIntervals(size: Int) = for {
    length <- 1 to size
    interval <- intervals(length, size)
  } yield interval
  
  def splits(interval: Interval) = for {
    k <- (interval.head + 1) until interval.end
    i = interval.head
    j = interval.end
  } yield (Interval.open(i, k), Interval.open(k, j))

}