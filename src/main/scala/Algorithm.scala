package org.example.vanilla

import com.salesforce.op.{OpWorkflow, OpWorkflowModel}
import org.apache.predictionio.controller.{P2LAlgorithm, Params, PersistentModel, PersistentModelLoader}
import org.apache.spark.SparkContext
import grizzled.slf4j.Logger
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types._
import com.salesforce.op.local._
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType, StructField, StructType}

// TODO Implement batch method using Spark

case class AlgorithmParams(target: String, schema: Seq[Field]) extends Params {
  def structType: StructType = {
    StructType(
      schema.map { field =>
        StructField(field.field, field.`type` match {
          case "double" => DoubleType
          case "string" => StringType
          case "int"    => IntegerType
        })
      }
    )
  }
}
case class Field(field: String, `type`: String)

class Algorithm(val params: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Map[String, Any], PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): Model = {

    val spark = SparkSession.builder.config(sc.getConf).getOrCreate()

    val df = spark.createDataFrame(data.events.map { event =>
      Row(
        event.properties.get[String]("survived").toDouble,
        toOption(event.properties.get[String]("pClass")).map(_.toString).getOrElse(""),
        toOption(event.properties.get[String]("name")).getOrElse(""),
        toOption(event.properties.get[String]("sex")).getOrElse(""),
        toOption(event.properties.get[String]("age")).map(_.toDouble).getOrElse(0d),
        toOption(event.properties.get[String]("sibSp")).map(_.toInt).getOrElse(0),
        toOption(event.properties.get[String]("parCh")).map(_.toInt).getOrElse(0),
        toOption(event.properties.get[String]("ticket")).getOrElse(""),
        toOption(event.properties.get[String]("fare")).map(_.toDouble).getOrElse(0d),
        toOption(event.properties.get[String]("cabin")).getOrElse(""),
        toOption(event.properties.get[String]("embarked")).getOrElse("")
      )
    }, params.structType)

    val (target, features) = FeatureBuilder.fromDataFrame[RealNN](df, params.target)
    val featureVector = features.toSeq.autoTransform()
    val checkedFeatures = target.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)

    val workflow =
      new OpWorkflow()
        .setResultFeatures(target, checkedFeatures)
        .setInputDataset(df)

    val fittedWorkflow = workflow.train()(spark)

    new Model(fittedWorkflow, fittedWorkflow.scoreFunction(spark))
  }

  private def toOption(s: String): Option[String] = {
    if(s.isEmpty) None else Some(s)
  }

  def predict(model: Model, query: Map[String, Any]): PredictedResult = {
//    val map = Map(
//      "survived" -> 0,
//      "pClass"   -> query.pClass.getOrElse(""),
//      "name"     -> query.name.getOrElse(""),
//      "sex"      -> query.sex.getOrElse(""),
//      "age"      -> query.age.getOrElse(0),
//      "sibSp"    -> query.sibSp.getOrElse(0),
//      "parCh"    -> query.parCh.getOrElse(0),
//      "ticket"   -> query.ticket.getOrElse(""),
//      "fare"     -> query.fare.getOrElse(0),
//      "cabin"    -> query.cabin.getOrElse(""),
//      "embarked" -> query.embarked.getOrElse("")
//    )
    println("***************")
    println(query)
    println("***************")
//    val result = model.scoreFunction(map)
//    PredictedResult(result(prediction.name).asInstanceOf[Map[String, Any]]("prediction").asInstanceOf[Double].toInt)
    PredictedResult(0)
  }
}

class Model(val model: OpWorkflowModel, val scoreFunction: ScoreFunction) extends PersistentModel[AlgorithmParams] {

  private lazy val logger = Logger[this.type]

  override def save(id: String, params: AlgorithmParams, sc: SparkContext): Boolean = {
    val path = "/tmp/" + id + ".model"
    model.save(path, true)
    logger.info(s"Saved model to $path")
    true
  }

}

object Model extends PersistentModelLoader[AlgorithmParams, Model] {

  override def apply(id: String, params: AlgorithmParams, sc: Option[SparkContext]): Model = {
    try {
      val path = "/tmp/" + id + ".model"

      val spark = SparkSession.builder.config(sc.get.getConf).getOrCreate()

      val df = spark.createDataFrame(sc.get.emptyRDD[Row], params.structType)
      val (target, features) = FeatureBuilder.fromDataFrame[RealNN](df, params.target)
      val featureVector = features.toSeq.autoTransform()
      val checkedFeatures = target.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)

      val workflow = new OpWorkflow()
        .setResultFeatures(target, checkedFeatures)
        .loadModel(path)

      new Model(workflow, workflow.scoreFunction(spark))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

}
