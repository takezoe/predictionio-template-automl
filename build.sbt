name := "template-scala-automl"

scalaVersion := "2.11.12"
val sparkV = "2.3.2"
val aiV = "0.6.0"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.14.0" % "provided",
  "org.apache.spark"        %% "spark-mllib"              % sparkV % "provided",
  "com.salesforce.transmogrifai" %% "transmogrifai-core" % aiV excludeAll(
    ExclusionRule(organization = "org.apache.spark")
  ),
  "com.salesforce.transmogrifai" %% "transmogrifai-local" % aiV excludeAll(
    ExclusionRule(organization = "org.apache.spark")
  ),
  "com.salesforce.transmogrifai" %% "transmogrifai-features" % aiV  excludeAll(
    ExclusionRule(organization = "org.apache.spark")
  ),
  "org.scalatest"           %% "scalatest"                % "3.0.5" % "test")

// SparkContext is shared between all tests via SharedSingletonContext
parallelExecution in Test := false

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "reference.conf"       => MergeStrategy.concat
  case _                      => MergeStrategy.first
}

