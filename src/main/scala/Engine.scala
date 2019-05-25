package org.example.vanilla

import org.apache.predictionio.controller.EngineFactory
import org.apache.predictionio.controller.Engine

//case class Query(
//  pClass: Option[Int],
//  name: Option[String],
//  sex: Option[String],
//  age: Option[Double],
//  sibSp: Option[Int],
//  parCh: Option[Int],
//  ticket: Option[String],
//  fare: Option[Double],
//  cabin: Option[String],
//  embarked: Option[String]
//) extends Serializable

case class PredictedResult(survived: Int) extends Serializable

object VanillaEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("algo" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
