# AutoML Engine Template

This is a [Apache PredictionIO](https://predictionio.apache.org/) engine template which offers AutoML capability using [TransmogrifAI](https://transmogrif.ai/).

You can launch a prediction WebAPI service without any coding.

```
$ curl -H "Content-Type: application/json" -d '{ "pClass": "3", "name": "Glynn, Miss. Mary Agatha", "sex": "female", "age": 66, "sibSp": 1, "parCh": 0, "ticket": "C.A", "fare", 1.25, "cabin": "", "embarked": "S" }' http://localhost:8000/queries.json -s | jq .
```

