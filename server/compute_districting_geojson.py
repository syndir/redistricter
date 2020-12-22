#!/usr/bin/env python3
#
# compute_districting_geojson.py [json to compute from] [districting file]


import shapely
from shapely.geometry import *
from shapely.ops import unary_union
import json
import sys


json_to_process = sys.argv[1]
districting_file = sys.argv[2]
outfile = sys.argv[3]

computed_districts = {}
districtings = {}
original_precincts = {}

with open(districting_file, 'r') as f:
    districtings = json.load(f)

original_geo = {}
with open(json_to_process, 'r') as f:
    original_geo = json.load(f)

for precinct in original_geo['features']:
    geoid = precinct['properties']['GEOID10']
    if original_precincts.get(geoid):
        original_precincts[geoid].append(precinct['geometry'])
    else:
        original_precincts[geoid] = [ precinct['geometry'] ]

computed_districts['average'] = {}
computed_districts['random'] = {}
computed_districts['extreme1'] = {}
computed_districts['extreme2'] = {}

for district in districtings['average']:
    computed_districts['average'][district] = []
    for precinct in districtings['average'][district]:
        if original_precincts[precinct] is not None:
            for p in original_precincts[precinct]:
                if p is not None:
                    computed_districts['average'][district].append(shape(p))
    computed_districts['average'][district] = unary_union(computed_districts['average'][district])

for district in districtings['random']:
    computed_districts['random'][district] = []
    for precinct in districtings['random'][district]:
        if original_precincts[precinct] is not None:
            for p in original_precincts[precinct]:
                if p is not None:
                    computed_districts['random'][district].append(shape(p))
    computed_districts['random'][district] = unary_union(computed_districts['random'][district])

for district in districtings['extreme1']:
    computed_districts['extreme1'][district] = []
    for precinct in districtings['extreme1'][district]:
        if original_precincts[precinct] is not None:
            for p in original_precincts[precinct]:
                if p is not None:
                    computed_districts['extreme1'][district].append(shape(p))
    computed_districts['extreme1'][district] = unary_union(computed_districts['extreme1'][district])

for district in districtings['extreme2']:
    computed_districts['extreme2'][district] = []
    for precinct in districtings['extreme2'][district]:
        if original_precincts[precinct] is not None:
            for p in original_precincts[precinct]:
                if p is not None:
                    computed_districts['extreme2'][district].append(shape(p))
    computed_districts['extreme2'][district] = unary_union(computed_districts['extreme2'][district])

output = {}
output['average'] = {}
output['random'] = {}
output['extreme1'] = {}
output['extreme2'] = {}

for x in computed_districts['average']:
    neighbors = []
    for adj in computed_districts['average']:
        if computed_districts['average'][adj] is not computed_districts['average'][x] and computed_districts['average'][x].touches(computed_districts['average'][adj]):
            neighbors.append(adj)
    output['average'][x] = [ {'type' : 'Feature', 'properties' : { 'neighbors' : neighbors }, 'geometry' : shapely.geometry.mapping(computed_districts['average'][x])}]

for x in computed_districts['random']:
    neighbors = []
    for adj in computed_districts['random']:
        if computed_districts['random'][adj] is not computed_districts['random'][x] and computed_districts['random'][x].touches(computed_districts['random'][adj]):
            neighbors.append(adj)
    output['random'][x] = [ {'type' : 'Feature', 'properties' : { 'neighbors' : neighbors }, 'geometry' : shapely.geometry.mapping(computed_districts['random'][x])}]

for x in computed_districts['extreme1']:
    neighbors = []
    for adj in computed_districts['extreme1']:
        if computed_districts['extreme1'][adj] is not computed_districts['extreme1'][x] and computed_districts['extreme1'][x].touches(computed_districts['extreme1'][adj]):
            neighbors.append(adj)
    output['extreme1'][x] = [ {'type' : 'Feature', 'properties' : { 'neighbors' : neighbors }, 'geometry' : shapely.geometry.mapping(computed_districts['extreme1'][x])}]

for x in computed_districts['extreme2']:
    neighbors = []
    for adj in computed_districts['extreme2']:
        if computed_districts['extreme2'][adj] is not computed_districts['extreme2'][x] and computed_districts['extreme2'][x].touches(computed_districts['extreme2'][adj]):
            neighbors.append(adj)
    output['extreme2'][x] = [ {'type' : 'Feature', 'properties' : { 'neighbors' : neighbors }, 'geometry' : shapely.geometry.mapping(computed_districts['extreme2'][x])}]

with open(outfile, 'w') as f:
    json.dump(output, f)