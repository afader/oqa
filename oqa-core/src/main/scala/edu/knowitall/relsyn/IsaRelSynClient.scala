package edu.knowitall.relsyn

import edu.knowitall.util.ResourceUtils
import edu.knowitall.execution.TConjunct

object IsaRelSynClient extends RelSynClient {
  lazy val client = ListRelSynClient.fromInputStream(ResourceUtils.resource("/edu/knowitall/search/qa/isa.txt"))
  override def relSyns(c: TConjunct) = client.relSyns(c)
}