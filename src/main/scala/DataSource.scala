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
      EmptyEvaluationInfo, Map[String, Any], EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {

   val eventsRDD: RDD[Event] = PEventStore.find(
      appName = dsp.appName,
      entityType = Some("passenger"),
      eventNames = Some(List("titanic")))(sc)

//   val passengersRDD = eventsRDD.map { event =>
//      Passenger(
//        event.entityId.toInt,
//        event.properties.get[String]("survived").toInt,
//        toOption(event.properties.get[String]("pClass")).map(_.toString),
//        toOption(event.properties.get[String]("name")),
//        toOption(event.properties.get[String]("sex")),
//        toOption(event.properties.get[String]("age")).map(_.toDouble),
//        toOption(event.properties.get[String]("sibSp")).map(_.toInt),
//        toOption(event.properties.get[String]("parCh")).map(_.toInt),
//        toOption(event.properties.get[String]("ticket")),
//        toOption(event.properties.get[String]("fare")).map(_.toDouble),
//        toOption(event.properties.get[String]("cabin")),
//        toOption(event.properties.get[String]("embarked"))
//      )
//    }

    new TrainingData(eventsRDD)
  }

//  private def toOption(s: String): Option[String] = {
//    if(s.isEmpty) None else Some(s)
//  }
}

class TrainingData(
  val events: RDD[Event]
) extends Serializable {
  override def toString = {
    s"events: [${events.count()}] (${events.take(2).toList}...)"
  }
}

//case class Passenger(
//  id: Int,
//  survived: Int,
//  pClass: Option[String],
//  name: Option[String],
//  sex: Option[String],
//  age: Option[Double],
//  sibSp: Option[Int],
//  parCh: Option[Int],
//  ticket: Option[String],
//  fare: Option[Double],
//  cabin: Option[String],
//  embarked: Option[String]
//)
