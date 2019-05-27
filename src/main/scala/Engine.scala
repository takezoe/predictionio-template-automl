package org.example.vanilla

import org.apache.predictionio.controller.EngineFactory
import org.apache.predictionio.controller.Engine

case class PredictedResult(survived: Double) extends Serializable

object VanillaEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("algo" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
