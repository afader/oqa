package edu.knowitall.util

import java.io.InputStream
import scala.io.Source

object ResourceUtils {
  def resource(path: String): InputStream = {
    val stream = getClass.getResourceAsStream(path)
    if (stream != null) {
      stream
    } else {
      throw new IllegalStateException(s"could not load resource $path")
    }
  }
  def resourceSource(path: String) = Source.fromInputStream(resource(path), "UTF-8")
}