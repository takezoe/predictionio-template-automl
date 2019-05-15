package org.example.vanilla

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EmptyActualResult
import org.apache.predictionio.controller.Params
import org.apache.predictionio.data.storage.Event
import org.apache.predictionio.data.store.PEventStore

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {

   val eventsRDD: RDD[Event] = PEventStore.find(
      appName = dsp.appName,
      entityType = Some("ENTITY_TYPE"),
      eventNames = Some(List("EVENT")),
      targetEntityType = Some(Some("TARGET_ENTITY_TYPE")))(sc)

   val passengersRDD = eventsRDD.map { event =>
      Passenger(
        event.properties.get("id"),
        event.properties.get("survived"),
        event.properties.get("pClass"),
        event.properties.get("name"),
        event.properties.get("sex"),
        event.properties.get("age"),
        event.properties.get("sibSp"),
        event.properties.get("parCh"),
        event.properties.get("ticket"),
        event.properties.get("fare"),
        event.properties.get("cabin"),
        event.properties.get("embarked")
      )
    }

    new TrainingData(passengersRDD)
  }
}

class TrainingData(
  val events: RDD[Passenger]
) extends Serializable {
  override def toString = {
    s"events: [${events.count()}] (${events.take(2).toList}...)"
  }
}

case class Passenger(
  id: Int,
  survived: Int,
  pClass: Option[Int],
  name: Option[String],
  sex: Option[String],
  age: Option[Double],
  sibSp: Option[Int],
  parCh: Option[Int],
  ticket: Option[String],
  fare: Option[Double],
  cabin: Option[String],
  embarked: Option[String]
)