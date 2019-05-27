"""
Import sample data for recommendation engine
"""

import predictionio
import argparse
import random
import csv

RATE_ACTIONS_DELIMITER = "::"
SEED = 3

def import_events(client, file):
  f = open(file, 'r')
  reader = csv.reader(f)

  count = 0
  print("Importing data...")
  header = next(reader)
  for row in reader:
    properties = {
      "survived": float(row[1]),
      "pClass": row[2],
      "name": row[3],
      "sex": row[4],
      "age": None if row[5] == '' else float(row[5]),
      "sibSp": None if row[6] == '' else int(row[6]),
      "parCh": None if row[7] == '' else int(row[7]),
      "ticket": row[8],
      "fare": None if row[9] == '' else float(row[9]),
      "cabin": row[10],
      "embarked": row[11]
    }
    print(properties)
    client.create_event(
      event = "titanic",
      entity_type = "passenger",
      entity_id = row[0],
      properties = properties
    )
    count += 1
  f.close()
  print("%s events are imported." % count)

if __name__ == '__main__':
  parser = argparse.ArgumentParser(
    description="Import sample data for recommendation engine")
  parser.add_argument('--access_key', default='invald_access_key')
  parser.add_argument('--url', default="http://localhost:7070")
  parser.add_argument('--file', default="./data/titanic.csv")

  args = parser.parse_args()
  print(args)

  client = predictionio.EventClient(
    access_key=args.access_key,
    url=args.url,
    threads=5,
    qsize=500)
  import_events(client, args.file)