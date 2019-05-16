package org.example.vanilla

import com.salesforce.op.{OpWorkflow, OpWorkflowModel}
import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.predictionio.controller.Params
import org.apache.spark.SparkContext
import grizzled.slf4j.Logger
//import com.salesforce.op._
import com.salesforce.op.evaluators.Evaluators
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types._
import com.salesforce.op.local._
import com.salesforce.op.local.OpWorkflowModelLocal
import com.salesforce.op.readers.DataReaders
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelSelector
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelsToTry._
import com.salesforce.op.local._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

case class AlgorithmParams(mult: Int) extends Params

// TODO Implement batch method using Spark

class Algorithm(val ap: AlgorithmParams)
  // extends PAlgorithm if Model contains RDD[]
  extends P2LAlgorithm[PreparedData, ScoreFunction, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): ScoreFunction = {
    val survived = FeatureBuilder.RealNN[Passenger].extract(_.survived.toRealNN).asResponse
    val pClass = FeatureBuilder.PickList[Passenger].extract(_.pClass.map(_.toString).toPickList).asPredictor
    val name = FeatureBuilder.Text[Passenger].extract(_.name.toText).asPredictor
    val sex = FeatureBuilder.PickList[Passenger].extract(_.sex.map(_.toString).toPickList).asPredictor
    val age = FeatureBuilder.Real[Passenger].extract(_.age.toReal).asPredictor
    val sibSp = FeatureBuilder.Integral[Passenger].extract(_.sibSp.toIntegral).asPredictor
    val parCh = FeatureBuilder.Integral[Passenger].extract(_.parCh.toIntegral).asPredictor
    val ticket = FeatureBuilder.PickList[Passenger].extract(_.ticket.map(_.toString).toPickList).asPredictor
    val fare = FeatureBuilder.Real[Passenger].extract(_.fare.toReal).asPredictor
    val cabin = FeatureBuilder.PickList[Passenger].extract(_.cabin.map(_.toString).toPickList).asPredictor
    val embarked = FeatureBuilder.PickList[Passenger].extract(_.embarked.map(_.toString).toPickList).asPredictor

    // 特徴ベクトルを作成
    val passengerFeatures = Seq(
      pClass, name, sex, age, sibSp, parCh, ticket, fare,
      cabin, embarked
    ).transmogrify()

    // モデルを定義
    val prediction =
      BinaryClassificationModelSelector.withTrainValidationSplit(
        modelTypesToUse = Seq(OpLogisticRegression)
      ).setInput(survived, passengerFeatures).getOutput()

    // ワークフローを定義
    val workflow =
      new OpWorkflow()
        .setResultFeatures(survived, prediction)
        .setInputRDD(data.events)

    // 学習
    val spark = SparkSession.builder.config(sc.getConf).getOrCreate()
    val fittedWorkflow = workflow.train()(spark)

    fittedWorkflow.scoreFunction(spark)
  }

  def predict(model: ScoreFunction, query: Query): PredictedResult = {
    val map = Map(
      "id"       -> query.id,
      //"survived" -> query.survived,
      "pClass"   -> query.pClass,
      "name"     -> query.name,
      "sex"      -> query.sex,
      "age"      -> query.age,
      "sibSp"    -> query.sibSp,
      "parCh"    -> query.parCh,
      "ticket"   -> query.ticket,
      "fare"     -> query.fare,
      "cabin"    -> query.cabin,
      "embarked" -> query.embarked
    )
    val result = model(map)
    PredictedResult(result("survived").toString.toInt) // TODO cast
  }
}
