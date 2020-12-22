#!/bin/python
#
# Merges a neighbors file with the population data contained in a GeoJSON file.
# This will result in a file containing the data necessary for the algorithm to
# execute.

import json
import sys


if len(sys.argv) != 4:
    print(f"{sys.argv[0]} [neighbors file] [geojson file] [output file]")
    sys.exit(0)

neighbors_file = sys.argv[1]
geojson_file = sys.argv[2]
outfile = sys.argv[3]

neighbors = {}
geojson = {}

with open(neighbors_file, 'r') as f:
    neighbors = json.load(f)
with open(geojson_file, 'r') as f:
    geojson = json.load(f)

for feature in geojson['features']:
    record_to_update = neighbors[feature['properties']['GEOID10']]
    neighbors[feature['properties']['GEOID10']]['population'] = feature['properties']['T_POP']

with open(outfile, 'w') as f:
    json.dump(neighbors, f, indent=2)
