package org.example.vanilla

import org.apache.predictionio.controller.LServing

class Serving
  extends LServing[Map[String, Any], PredictedResult] {

  override
  def serve(query: Map[String, Any],
    predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
