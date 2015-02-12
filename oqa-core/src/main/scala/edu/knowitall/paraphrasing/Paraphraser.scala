package edu.knowitall.paraphrasing


trait Paraphraser {
  def paraphrase(s: String): List[Paraphrase]
  def paraphraseToStrings(s: String): List[String] =
    paraphrase(s).map(pp => pp.target)
}

object EmptyParaphraser extends Paraphraser {
  override def paraphrase(s: String) = List()
}

object IdentityParaphraser extends Paraphraser {
  override def paraphrase(s: String) = List(Paraphrase(s, s, IdentityDerivation))
}