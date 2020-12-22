#!/usr/bin/env python3
#
# Given a CD block assignment file, a VTD file, and a json file that results
# from the execution of the `compute_pop.py` script, this will create a .cd JSON
# file which contains a dictionary where the keys are the district numbers and
# the values are a list of precincts (by GEOID10 tag) contained in each district.

import sys
import json
import csv
import collections


if len(sys.argv) != 5:
    print(f"{sys.argv[0]} [CD assignment file] [VTD assignment file] [JSON to join against] [output file]")
    sys.exit(0)

cd_file = sys.argv[1]
vtd_file = sys.argv[2]
json_file = sys.argv[3]
output_file = sys.argv[4]

data = {}
raw_json = {}
districting = {}

# load and merge the two block assignment files
with open(cd_file, 'r') as f:
    csv_file = csv.DictReader(f)
    for row in csv_file:
        new_cd = {}
        new_cd['district'] = row['BLOCKID'][0:2] + row['DISTRICT']
        data[row['BLOCKID']] = new_cd
with open(vtd_file, 'r') as f:
    csv_file = csv.DictReader(f)
    for row in csv_file:
        data[row['BLOCKID']]['county'] = row['COUNTYFP']
        data[row['BLOCKID']]['vtd'] = row['DISTRICT']

with open(json_file, 'r') as f:
    raw_json = json.load(f)

# we only really care about the info associated with the features
# so we'll create a dict keyed by county id, wherein each county will contain
# a list of <vtd, geoid> records as its value
json_data = {}
features = raw_json['features']
for feature in features:
    if not json_data.get(feature['properties']['COUNTYFP10']):
        json_data[feature['properties']['COUNTYFP10']] = {}

    stripped_record = {}
    stripped_record['county'] = feature['properties']['COUNTYFP10']
    stripped_record['vtd'] = feature['properties']['VTDST10']
    stripped_record['geoid'] = feature['properties']['GEOID10']
    if not json_data[feature['properties']['COUNTYFP10']].get('records'):
        json_data[feature['properties']['COUNTYFP10']]['records'] = []

    json_data[feature['properties']['COUNTYFP10']]['records'].append(stripped_record)

for block in data:
    for record in json_data[data[block]['county']]['records']:
        if record['vtd'] == data[block]['vtd'] and record['county'] == data[block]['county']:
            if not districting.get(data[block]['district']):
                districting[data[block]['district']] = []
            if record['geoid'] not in districting[data[block]['district']]:
                districting[data[block]['district']].append(record['geoid'])

            # we can remove the entry to slowly slim the data set we need to pass
            # through
            json_data[data[block]['county']]['records'].remove(record)
            break


records_left = 0
for record in json_data:
    records_left += len(json_data[record]['records'])

with open(output_file, 'w') as f:
    json.dump(collections.OrderedDict(sorted(districting.items())), f, indent=2)

print(f"Wrote {len(districting)} records\n" \
      f"Unassociated records: {records_left}")
