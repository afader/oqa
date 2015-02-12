scalaVersion := "2.10.2"

organization := "edu.knowitall.oqa"

name := "oqa"

version := "0.1-SNAPSHOT"

fork in run := true

javaOptions in run += "-Xmx8G"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.slf4j" % "slf4j-log4j12" % "1.7.10",
  "com.typesafe" % "config" % "1.0.2",
  "edu.washington.cs.knowitall.nlptools" %% "nlptools-postag-stanford" % "2.4.5",
  "edu.washington.cs.knowitall.nlptools" %% "nlptools-tokenize-breeze" % "2.4.5",
  "edu.washington.cs.knowitall.nlptools" %% "nlptools-stem-morpha" % "2.4.5",
  "edu.washington.cs.knowitall.nlptools" %% "nlptools-tokenize-clear" % "2.4.5",
  "edu.washington.cs.knowitall.nlptools" %% "nlptools-chunk-opennlp" % "2.4.5",
  "edu.washington.cs.knowitall.taggers" %% "taggers-core" % "0.4",
  "com.rockymadden.stringmetric" % "stringmetric-core" % "0.25.3",
  "org.apache.solr" % "solr-solrj" % "4.3.1",
  "com.twitter" %% "util-collection" % "6.3.6",
  "org.scalaj" %% "scalaj-http" % "0.3.10",
  "commons-logging" % "commons-logging" % "1.2"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-actors" % _)
