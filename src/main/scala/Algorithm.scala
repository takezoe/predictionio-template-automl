package org.example.vanilla

import com.salesforce.op.{OpWorkflow, OpWorkflowModel}
import org.apache.predictionio.controller.{EmptyParams, P2LAlgorithm, PersistentModel, PersistentModelLoader}
import org.apache.spark.SparkContext
import grizzled.slf4j.Logger
import org.slf4j.LoggerFactory
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types._
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelSelector
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelsToTry._
import com.salesforce.op.local._
import org.apache.spark.sql.SparkSession

// TODO Implement batch method using Spark

class Algorithm(val ap: EmptyParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] {

  import Model._

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): Model = {
    val workflow =
      new OpWorkflow()
        .setResultFeatures(survived, prediction)
        .setInputRDD(data.events)

    val spark = SparkSession.builder.config(sc.getConf).getOrCreate()
    val fittedWorkflow = workflow.train()(spark)

    new Model(fittedWorkflow, fittedWorkflow.scoreFunction(spark))

//    try {
//      val f = fittedWorkflow.scoreFunction(spark)
//      val out = new ByteArrayOutputStream()
//      val bout = new ObjectOutputStream(out)
//      bout.writeObject(f)
//      bout.close()
//      out.close()
//
//      println("**************************")
//      // 0,3,"Braund, Mr. Owen Harris",male,22,1,0,A/5 21171,7.25,,S
//      println(f(Map(
//        "survived" -> 0,
//        "pClass"   -> "3",
//        "name"     -> "Braund, Mr. Owen Harris",
//        "sex"      -> "male",
//        "age"      -> 22,
//        "sibSp"    -> 1,
//        "parCh"    -> 0,
//        "ticket"   -> "A/5 21171",
//        "fare"     -> 7.25,
//        "cabin"    -> "",
//        "embarked" -> "S"
//      )))
//      println("**************************")
//    } catch {
//      case e: Exception =>
//        e.printStackTrace()
//        println("Error here!")
//    }
//    throw new RuntimeException("** here **")
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val map = Map(
      "survived" -> -1,
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
    PredictedResult(0)
    val result = model.scoreFunction(map)
    PredictedResult(result("survived").toString.toInt)
  }
}

class Model(val model: OpWorkflowModel, val scoreFunction: ScoreFunction) extends PersistentModel[EmptyParams] {

  private lazy val logger = Logger[this.type]

  override def save(id: String, params: EmptyParams, sc: SparkContext): Boolean = {
    val path = "/tmp/" + id + ".model"
    model.save(path, true)
    logger.info(s"Saved model to $path")
    true
  }

}

object Model extends PersistentModelLoader[EmptyParams, Model] {
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

  val passengerFeatures = Seq(
    pClass, name, sex, age, sibSp, parCh, ticket, fare,
    cabin, embarked
  ).transmogrify()

  val prediction =
    BinaryClassificationModelSelector.withTrainValidationSplit(
      modelTypesToUse = Seq(OpLogisticRegression)
    ).setInput(survived, passengerFeatures).getOutput()

  override def apply(id: String, params: EmptyParams, sc: Option[SparkContext]): Model = {
    try {
      val path = "/tmp/" + id + ".model"
      val workflow = new OpWorkflow()
        .setResultFeatures(survived, prediction)
        .loadModel(path)

      val spark = SparkSession.builder.config(sc.get.getConf).getOrCreate()
      new Model(workflow, workflow.scoreFunction(spark))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw e
    }
  }

}
