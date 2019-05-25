# Skeleton Engine Template

```
$ curl -H "Content-Type: application/json" -d '{ "pClass": "3", "name": "Glynn, Miss. Mary Agatha", "sex": "female", "age": 66, "sibSp": 1, "parCh": 0, "ticket": "C.A", "fare", 1.25, "cabin": "", "embarked": "S" }' http://localhost:8000/queries.json -s | jq .
```

## Documentation

Please refer to https://predictionio.apache.org/templates/vanilla/quickstart/

## Versions

### v0.14.0

Update for Apache PredictionIO 0.14.0

### v0.13.0

Update for Apache PredictionIO 0.13.0

### v0.12.0-incubating

- Bump version number to track PredictionIO version
- Sets default build targets according to PredictionIO

### v0.11.0-incubating

- Bump version number to track PredictionIO version
- Added basic tests
- Rename Scala package name
- Update SBT version

### v0.4.0

- Update for Apache PredictionIO 0.10.0-incubating

### v0.3.0

- update for PredictionIO 0.9.2, including:

  - use new PEventStore API
  - use appName in DataSource parameter


### v0.2.0

- update for PredictionIO 0.9.2

### v0.1.1

- update build.sbt and template.json for PredictionIO 0.9.2

### v0.1.0

- initial version
