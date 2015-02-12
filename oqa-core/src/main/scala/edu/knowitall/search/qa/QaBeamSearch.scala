package edu.knowitall.search.qa

import com.typesafe.config.ConfigFactory
import edu.knowitall.search.SingleBeam
import edu.knowitall.search.TypedBeams
import edu.knowitall.search.Beam
import edu.knowitall.search.BeamSearch

class QaBeamSearch(problem: QaSearchProblem) {
  private var t0 = 0L
  def search = {
    val beam = QaBeamSearch.newBeam
    val beamSearch = new BeamSearch(problem, beam)
    t0 = System.currentTimeMillis
    beamSearch.search
  }
  def startTime = t0 
}

object QaBeamSearch {
  val conf = ConfigFactory.load()
  val defaultBeamSize = conf.getInt("search.beamSize")
  val defaultBeamType = conf.getString("search.beamType")
  def newBeam: Beam[QaState, QaAction] = defaultBeamType match {
    case "single" => new SingleBeam[QaState, QaAction](defaultBeamSize)
    case "typed" => new TypedBeams[QaState, QaAction, String](x => x.state.stateType, defaultBeamSize)
    case _ => throw new IllegalStateException(s"Invalid beam type: defaultBeamType")
  }
}