name := "template-scala-automl"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.14.0" % "provided",
  "org.apache.spark"        %% "spark-mllib"              % "2.4.0" % "provided",
  "com.salesforce.transmogrifai" %% "transmogrifai-core" % "0.5.3" excludeAll(
    ExclusionRule(organization = "org.apache.spark")
  ),
  "com.salesforce.transmogrifai" %% "transmogrifai-local" % "0.5.3" excludeAll(
    ExclusionRule(organization = "org.apache.spark")
  ),
  "com.salesforce.transmogrifai" %% "transmogrifai-features" % "0.5.3"  excludeAll(
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