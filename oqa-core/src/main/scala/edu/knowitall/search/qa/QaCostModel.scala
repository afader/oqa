package edu.knowitall.search.qa

import edu.knowitall.learning.SparseVector
import com.typesafe.config.ConfigFactory
import scala.io.Source
import java.io.File


class QaCostModel(
    var features: Function[QaStep, SparseVector] = QaCostModel.defaultFeatures,
    var weights: SparseVector = QaCostModel.defaultWeights) extends Function[QaStep, Double] {
  // multiply by -1 since search algos find minimum path
  override def apply(step: QaStep) = -1.0 * (features(step) * weights)
}

object QaCostModel {
  val conf = ConfigFactory.load()
  val defaultFeatures = QaFeatures
  lazy val defaultWeights = if (conf.hasPath("scoring.weights")) { 
    SparseVector.fromFile(conf.getString("scoring.weights"))
  } else {
    val in = getClass.getResourceAsStream("/edu/knowitall/search/qa/defaultWeights.txt")
    SparseVector.fromInputStream(in)
    //SparseVector.zero
  }
  
}