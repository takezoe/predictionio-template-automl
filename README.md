# AutoML Engine Template

This is a [Apache PredictionIO](https://predictionio.apache.org/) engine template which offers AutoML capability using [TransmogrifAI](https://transmogrif.ai/).

You can launch a prediction WebAPI service without any coding.

## Run Titanic example

Run the event server.

```bash
$ pio eventserver &
```

Import data to the event server.

```bash
$ ./data/import_titanic.py ./data/titanic.csv
```

Train a model.

```bash
$ pio train
```

Deploy the trained model as Web API.

```bash
$ pio deploy
```

Test the Web API.

```bash
$ curl -H "Content-Type: application/json" -d '{ "pClass": "3", "name": "Glynn, Miss. Mary Agatha", "sex": "female", "age": 66, "sibSp": 1, "parCh": 0, "ticket": "C.A", "fare", 1.25, "cabin": "", "embarked": "S" }' http://localhost:8000/queries.json -s | jq .
{
  "survived": 1
}
```

## Customize

You only need to modify algorithm parameters in `engine.json` to customize this template.

```json
"algorithms": [
  {
    "name": "algo",
    "params": {
      "target" : "survived",
      "schema" : [
        {
          "field": "survived",
          "type": "double",
          "nullable": false
        },
        {
          "field": "pClass",
          "type": "string",
          "nullable": true
        },
        ...
      ]
    }
  }
]
```

Define `schema` according to your data, and specify `target` which will be a response of prediction Web API. Currently, this template supports only binary classification, so the target field must be a binary type.