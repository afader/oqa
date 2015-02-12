package edu.knowitall.lm

import java.net.URL
import java.net.URI
import java.net.URLEncoder
import scalaj.http.Http
import scala.io.Source
import scalaj.http.HttpOptions
import com.typesafe.config.ConfigFactory
import edu.knowitall.util.MathUtils
import org.slf4j.LoggerFactory
import com.twitter.util.LruMap
import scala.collection.mutable.SynchronizedMap

trait LanguageModel {
  /**
   * Queries a string, returns its log probability.
   */
  def query(s: String): Double
  /**
   * Does a batch query of a bunch of strings, returns a list of the (input,
   * log probability) pairs.
   */
  def query(s: Iterable[String]): List[(String, Double)]
}

case class KenLmServer(url: String, timeOut: Int, 
					   scale: Boolean = KenLmServer.scale,
					   cacheSize: Int = KenLmServer.defaultCacheSize,
					   skipTimeouts: Boolean = KenLmServer.defaultSkipTimeouts) extends LanguageModel {
  def this() = this(KenLmServer.defaultUrl, KenLmServer.defaultTimeout)
  val logger = LoggerFactory.getLogger(this.getClass)
  val root = s"${url}/score"
  val retries = KenLmServer.retries
  
  private val cache = new LruMap[String, Double](cacheSize) with SynchronizedMap[String, Double]
  
  override def query(s: String): Double = cache.get(s) match {
    case Some(x) => x
    case None => try {
      val result = queryHelper(s)
      cache.put(s, result)
      result
    } catch {
      case e: IllegalStateException => if (skipTimeouts) {
        logger.warn(s"Could not compute LM score for '$s': $e")
        scaleValue(Double.MinValue)
      } else {
        throw e
      }
    }
  }
  
  private def queryHelper(s: String, attempt: Int = 0): Double = {
    if (attempt > retries) {
      throw new IllegalStateException(s"Unable to query KenLM for '$s'")
    } else {
      try {
        logger.debug(s"Querying for one string (attempt ${attempt+1}/$retries): $s")
        scaleValue(Http(root).option(HttpOptions.connTimeout(timeOut)).params("q" -> s).asString.toDouble)
      } catch {
        case e: Throwable => {
          queryHelper(s, attempt + 1)
        }
      }
    }
  }

  def queryBatch(s: Iterable[String]) = {
    logger.debug(s"Querying for ${s.size} strings")
    val lst = s.toList
    val joined = lst.mkString("|")
    val lines = Http.post(root).
    			option(HttpOptions.connTimeout(timeOut)).
    			option(HttpOptions.readTimeout(timeOut)).
    			params("q" -> joined).
    			asString.trim.split("\n")
    val results = lst.zip(lines).map { case (a, b) => (a, scaleValue(b.toDouble)) }
    for ((a, b) <- results) cache.put(a, b)
    results
  }
  override def query(s: Iterable[String]) = {
    val groups = s.grouped(KenLmServer.batchSize)
    groups.flatMap(queryBatch).toList
  }
  private def scaleValue(x: Double): Double = 
    if (scale) MathUtils.clipScale(x, KenLmServer.minValue, KenLmServer.maxValue)
    else x
}

case object KenLmServer {
  val conf = ConfigFactory.load()
  val defaultUrl = conf.getString("lm.url")
  val defaultTimeout = conf.getInt("lm.timeout")
  val retries = conf.getInt("lm.retries")
  val batchSize = conf.getInt("lm.batchSize");
  val minValue = conf.getDouble("lm.minValue")
  val maxValue = conf.getDouble("lm.maxValue")
  val scale = conf.getBoolean("lm.scale")
  val defaultSkipTimeouts = conf.getBoolean("lm.skipTimeouts")
  val defaultCacheSize = conf.getInt("lm.cacheSize")
}
