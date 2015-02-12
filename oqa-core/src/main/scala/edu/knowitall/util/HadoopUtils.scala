package edu.knowitall.util

object HadoopUtils {
  
  
  
  def groupIterator[S, T](iter: Iterator[T], f: (T => S)): Iterator[(S, Iterator[T])] =
    for (head <- iter; headVal = f(head); rest = iter.takeWhile(x => f(x) == headVal)) yield (headVal, List(head).iterator ++ rest)

}