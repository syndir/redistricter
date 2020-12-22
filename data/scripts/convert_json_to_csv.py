#!/usr/bin/env python3
#
# This is so that we can convert the .cd files to .csv files so that they can
# be merged with the shape files in QGIS to visually verify things

import sys
import json
import csv


if len(sys.argv) != 3:
    print(f"{sys.argv[0]} [json file] [csv file]")
    sys.exit(0)

json_file = sys.argv[1]
csv_file = sys.argv[2]

data = None

with open(json_file, 'r') as f:
    data = json.load(f)

with open(csv_file, 'w') as f:
    csv_writer = csv.writer(f)
    csv_writer.writerow(["district", "geoid"])

    for record in data:
        for column in data[record]:
            csv_writer.writerow([record, column])
