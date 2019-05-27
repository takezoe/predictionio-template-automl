package org.example.vanilla

import java.io.File

import com.salesforce.op.features.FeatureBuilder.fromRow
import com.salesforce.op.{OpWorkflow, OpWorkflowModel}
import org.apache.predictionio.controller.{P2LAlgorithm, Params, PersistentModel, PersistentModelLoader}
import org.apache.spark.SparkContext
import grizzled.slf4j.Logger
import com.salesforce.op.features.{Feature, FeatureSparkTypes}
import com.salesforce.op.features.types._
import com.salesforce.op.local._
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelSelector
import org.apache.commons.io.FileUtils
import org.apache.predictionio.data.storage.Event
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType, StructField, StructType}

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

// TODO Implement batch method using Spark

case class AlgorithmParams(target: String, schema: Seq[Field]) extends Params {
  def structType: StructType = {
    StructType(
      schema.map { field =>
        StructField(field.field, field.`type` match {
          case "string" => StringType
          case "double" => DoubleType
          case "int"    => IntegerType
        }, field.nullable)
      }
    )
  }

  def row(event: Event): Row = {
    Row(
      (schema.map { field =>
        val (value, default) = field.`type` match {
          case "string" => (event.properties.getOpt[String](field.field), "")
          case "double" => (event.properties.getOpt[Double](field.field), 0d)
          case "int"    => (event.properties.getOpt[Int](field.field), 0)
        }
        value match {
          case Some(x) => x
          case None    => if(field.nullable) null else default
        }
      }): _*
    )
  }

  // TODO Should be implemented in TransmogrifAI
  def features[ResponseType <: FeatureType : WeakTypeTag](
    nonNullable: Set[String] = Set.empty
  ): (Feature[ResponseType], Array[Feature[_ <: FeatureType]]) = {
    val schema = structType
    val allFeatures: Array[Feature[_ <: FeatureType]] =
      schema.fields.zipWithIndex.map { case (field, index) =>
        val isResponse = field.name == target
        val isNullable = !isResponse && !nonNullable.contains(field.name)
        val wtt: WeakTypeTag[_ <: FeatureType] = FeatureSparkTypes.featureTypeTagOf(field.dataType, isNullable)
        val feature = fromRow(name = field.name, index = Some(index))(wtt)
        if (isResponse) feature.asResponse else feature.asPredictor
      }
    val (responses, features) = allFeatures.partition(_.name == target)
    val responseFeature = responses.toList match {
      case feature :: Nil if feature.isSubtypeOf[ResponseType] =>
        feature.asInstanceOf[Feature[ResponseType]]
      case feature :: Nil =>
        throw new RuntimeException(
          s"Response feature '$target' is of type ${feature.typeName}, but expected ${FeatureType.typeName[ResponseType]}")
      case Nil =>
        throw new RuntimeException(s"Response feature '$target' was not found in dataframe schema")
      case _ =>
        throw new RuntimeException(s"Multiple features with name '$target' were found (should not happen): "
          + responses.map(_.name).mkString(","))
    }
    responseFeature -> features
  }

  def query(map: Map[String, Any]): Map[String, Any] = {
    map.map { case (key, value) =>
      key -> (value match {
        case x: BigInt => x.toInt
        case x         => x
      })
    } + (target -> 0d)
  }
}

case class Field(field: String, `type`: String, nullable: Boolean)

class Algorithm(val params: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Map[String, Any], PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  override def train(sc: SparkContext, data: PreparedData): Model = {

    val spark = SparkSession.builder.config(sc.getConf).getOrCreate()

    val df = spark.createDataFrame(data.events.map { event => params.row(event) }, params.structType)

    val (target, features) = params.features[RealNN]()
    val featureVector = features.toSeq.autoTransform()
    val checkedFeatures = target.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)
    val prediction = BinaryClassificationModelSelector().setInput(target, checkedFeatures).getOutput()

    val workflow =
      new OpWorkflow()
        .setResultFeatures(prediction)
        .setInputDataset(df)

    val fittedWorkflow = workflow.train()(spark)

    logger.info(fittedWorkflow.summaryPretty())

    new Model(prediction.name, fittedWorkflow, fittedWorkflow.scoreFunction(spark))
  }

  override def batchPredictBase(sc: SparkContext, bm: Any, qs: RDD[(Long, Map[String, Any])]): RDD[(Long, PredictedResult)] = {
    val model = bm.asInstanceOf[Model]

    val spark = SparkSession.builder.config(sc.getConf).getOrCreate()

    // TODO Is this thread safe?
    model.model.setInputRDD(qs.map { case (_, query) => query })
    val result = model.model.score()(spark).rdd

    result.zip(qs).map { case (row, (x, _)) =>
      (x, PredictedResult(row.get(row.fieldIndex(model.predictionName)).asInstanceOf[Map[String, Any]]("prediction").asInstanceOf[Double]))
    }
  }

  override def predict(model: Model, query: Map[String, Any]): PredictedResult = {
    logger.debug("query: " + query)

    val result = model.scoreFunction(params.query(query))
    logger.debug("result: " + result)

    PredictedResult(result(model.predictionName).asInstanceOf[Map[String, Any]]("prediction").asInstanceOf[Double])
  }
}

class Model(val predictionName: String, val model: OpWorkflowModel, val scoreFunction: ScoreFunction) extends PersistentModel[AlgorithmParams] {

  private lazy val logger = Logger[this.type]

  override def save(id: String, params: AlgorithmParams, sc: SparkContext): Boolean = {
    val path = "/tmp/" + id + ".model"
    model.save(path, true)
    FileUtils.writeStringToFile(new File(path + "/name"), predictionName, "UTF-8")
    logger.info(s"Saved model to $path")
    true
  }

}

object Model extends PersistentModelLoader[AlgorithmParams, Model] {

  override def apply(id: String, params: AlgorithmParams, sc: Option[SparkContext]): Model = {
    try {
      val path = "/tmp/" + id + ".model"

      val spark = SparkSession.builder.config(sc.get.getConf).getOrCreate()

      val (target, features) = params.features[RealNN]()
      val featureVector = features.toSeq.autoTransform()
      val checkedFeatures = target.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)
      val prediction = BinaryClassificationModelSelector().setInput(target, checkedFeatures).getOutput()

      val workflow = new OpWorkflow()
        .setResultFeatures(prediction)
        .loadModel(path)

      val predictionName = FileUtils.readFileToString(new File(path + "/name"), "UTF-8")

      new Model(predictionName, workflow, workflow.scoreFunction(spark))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

}
