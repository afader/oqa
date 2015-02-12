package edu.knowitall.util

import scala.actors.Futures

object TimingUtils {
  
  def time[R](block: => R): (Long, R) = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    (t1 - t0, result)
  }
  
  def runWithTimeout[T](timeoutMs: Long)(f: => T) : Option[T] = {
    Futures.awaitAll(timeoutMs, Futures.future(f)).head.asInstanceOf[Option[T]]
  }

}