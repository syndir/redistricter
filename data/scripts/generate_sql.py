#!/usr/bin/env python3
#
# Generates an sql file, which can be used by Spring to auto-populate the DB on
# startup.
#
# Takes a state abbreviation as input
# Requires the following files be present in the {ST}_data/ directory:
#   {ST}.cd                 -- current districting file (gen'd by
#                              compute_districting.py)
#   {ST}_outline.geojson    -- geojson file for outline of state boundary
#   {ST}_neighbors.json     -- json file containing precinct neighbor
#                              information
#   {ST}_db.geojson         -- geojson containing precinct boundary information
#                              and demographics

import sys
import json
from os import path


if len(sys.argv) != 3:
    print(f"{sys.argv[0]} [state abbreviation] [output file]")
    sys.exit(0)

abbrev = sys.argv[1]
output_file = sys.argv[2]

data_dir = abbrev + "_data/"
cd_file = data_dir + abbrev + ".cd"
outline_file = data_dir + abbrev + "_outline.geojson"
neighbors_file = data_dir + abbrev + "_neighbors.json"
db_file = data_dir + abbrev + "_db.geojson"

if not path.exists(data_dir):
    print(f"{data_dir} does not exist")
    sys.exit(0)

if not path.exists(cd_file) or not path.exists(outline_file) \
    or not path.exists(neighbors_file) or not path.exists(db_file):
        print("Missing required input files.")
        sys.exit(0)

cd = {}
outline = {}
neighbors = {}
db = {}

# load everything in
with open(cd_file, 'r') as cdf:
    cd = json.load(cdf)
with open(outline_file, 'r') as olf:
    outline = json.load(olf)
with open(neighbors_file, 'r') as nf:
    neighbors = json.load(nf)
with open(db_file, 'r') as dbf:
    db = json.load(dbf)

with open(output_file, 'w') as of:
    precinct_list = []

    # write the precincts table
    # precinct_id | precinct_name | county_name | county_id | total_pop_by_demo
    #   | vap_pop_by_demo | boundary | precinct_neighbors
    for p in db['features']:
        precinct = p['properties']
        precinct_list.append(precinct['GEOID10'])

        total_pop_by_demo = {}
        total_pop_by_demo['AFRICAN_AMERICAN'] = precinct['T_AA']
        total_pop_by_demo['ASIAN_AMERICAN'] = precinct['T_AS']
        total_pop_by_demo['HISPANIC'] = precinct['T_HISP']
        total_pop_by_demo['NATIVE_AMERICAN'] = precinct['T_AI']
        total_pop_by_demo['OTHER'] = precinct['T_OTH']
        total_pop_by_demo['ALL'] = precinct['T_POP']

        vap_pop_by_demo = {}
        vap_pop_by_demo['AFRICAN_AMERICAN'] = precinct['V_AA']
        vap_pop_by_demo['ASIAN_AMERICAN'] = precinct['V_AS']
        vap_pop_by_demo['HISPANIC'] = precinct['V_HISP']
        vap_pop_by_demo['NATIVE_AMERICAN'] = precinct['V_AI']
        vap_pop_by_demo['OTHER'] = precinct['V_OTH']
        vap_pop_by_demo['ALL'] = precinct['V_POP']

        precinct_neighbors = neighbors[precinct['GEOID10']]['neighbors']

        statement = f"INSERT INTO precincts VALUES ({json.dumps(precinct['GEOID10'])}, {json.dumps(precinct['NAME10'])}, {json.dumps(precinct['COUNTY'])}, {json.dumps(precinct['COUNTYFP10'])}, '{json.dumps(total_pop_by_demo)}', '{json.dumps(vap_pop_by_demo)}', '{json.dumps(p['geometry'])}', '{json.dumps(precinct_neighbors)}' );\n"
        of.write(statement)


    # insert districts
    district_list = []
    statement = f"INSERT INTO districts ( id, precincts ) VALUES"
    for district in cd:
        district_list.append(district)
        statement += f" ({json.dumps(district)}, '{json.dumps(cd[district])}'),"
    statement = statement.rstrip(",")
    statement += ";\n"
    of.write(statement)

    # insert districting plans
    statement = f"INSERT INTO districting_plans (districts, sorted_districts) VALUES ('{json.dumps(district_list)}', null);\n"
    of.write(statement)

    # insert state
    of.write(f"INSERT INTO states VALUES ({json.dumps(outline['statename'])}, LAST_INSERT_ID(), '{json.dumps(outline)}', '{json.dumps(precinct_list)}');\n")
