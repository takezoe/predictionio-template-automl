# AutoML Engine Template

This is a [Apache PredictionIO](https://predictionio.apache.org/) engine template which offers AutoML capability using [TransmogrifAI](https://transmogrif.ai/).

You can launch a prediction WebAPI service without any coding.

## Run Titanic example

Create an application.

```bash
$ pio app new MyAutoMLApp1
[INFO] [App$] Initialized Event Store for this app ID: 4.
[INFO] [Pio$] Created a new app:
[INFO] [Pio$]       Name: MyAutoMLApp1
[INFO] [Pio$]         ID: 1
[INFO] [Pio$] Access Key: xxxxxxxxxxxxxxxx
```

Set the accesskey to an environmental variable.

```bash
$ export ACCESS_KEY=xxxxxxxxxxxxxxxx
```

Run the event server.

```bash
$ pio eventserver &
[INFO] [Management$] Creating Event Server at 0.0.0.0:7070
```

Import data to the event server.

```bash
$ python ./data/import_titanic.py --file ./data/titanic.csv --access_key $ACCESS_KEY
890 events are imported.
```

Train a model. It can take a long time to find the best model.

```bash
$ pio train
```

Deploy the trained model as Web API.

```bash
$ pio deploy
```

Test the Web API.

```bash
$ curl -H "Content-Type: application/json" -d '{ "pClass": "2", "name": "Wheadon, Mr. Edward H", "sex": "male", "age": 66, "sibSp": 0, "parCh": 0, "ticket": "C.A 24579", "fare", 10.5, "cabin": "", "embarked": "S" }' http://localhost:8000/queries.json -s
{"survived":0.0}

$ curl -H "Content-Type: application/json" -d '{ "pClass": "2", "name": "Nicola-Yarred, Miss. Jamila", "sex": "female", "age": 14, "sibSp": 1, "parCh": 0, "ticket": "2651", "fare", 11.2417, "cabin": "", "embarked": "C" }' http://localhost:8000/queries.json -s
{"survived":1.0}
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