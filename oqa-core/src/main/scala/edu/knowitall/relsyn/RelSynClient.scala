package edu.knowitall.relsyn

import edu.knowitall.execution.TConjunct

trait RelSynClient {
  def relSyns(c: TConjunct): List[RelSynRule]
}