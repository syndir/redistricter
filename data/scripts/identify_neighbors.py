#!/usr/bin/env python3
#
# Requirements:
#   fiona
#   shapely
#   numpy
#
# Given a geojson as input, will compute the neighbors of all polygons
# and write them to a JSON output file in K/V pairs where
#   K -> the GEOID of the polygon
#   V -> a list of GEOIDs of its neighbors
#
# The input geojson must not include multipolygons. This is easiest to
# accomplish by using the 'explode' command in mapshaper, as it will split any
# multipolygons into single polygons while preserving the rest of the
# information associated with each record.

from shapely.strtree import STRtree
from shapely.geometry import *
import json
import fiona
import sys
import numpy as np


R = 6371                # radius of the earth, in km
BUFFER_SIZE = 0.06096   # 200ft in km
KM_TO_FEET = 3280.84    # 1km = 3280.84ft


# for fiona and geojson, records are stored lon/lat, not lat/lon
# returns in km
def get_cartesian(lat=None, lon=None):
    lat, lon = np.deg2rad(lat), np.deg2rad(lon)
    x = R * np.cos(lat) * np.cos(lon)
    y = R * np.cos(lat) * np.sin(lon)
    return x,y


if len(sys.argv) != 3:
   print(f"{sys.argv[0]} [input shapefile] [outfile]")
   sys.exit(0)

filename = sys.argv[1]
outfile = sys.argv[2]

neighbors_dict = {}
converted_shapes = []

# Step 1: Read shapefile/jsonfile of raw geometry and convert each polygon to
# a cartesion coordinate system and create a new Polygon from those X,Y points
orig_shapes = fiona.open(filename)
shape_iter = iter(orig_shapes)
current_shape = next(shape_iter)
print("Converting polygons to use X, Y coordinates...")
while current_shape is not None:
    print(f"  Converting {current_shape['properties']['GEOID10']}")
    for coord in current_shape['geometry']['coordinates']:
        new_shape = {}
        new_shape['geoid'] = current_shape['properties']['GEOID10']
        new_shape['points'] = []
        try:
            for x,y in coord:
                new_lat, new_lon = get_cartesian(y, x)
                new_point = Point(new_lon, new_lat)
                new_shape['points'].append(new_point)
        except ValueError as e:
            # this would only happen if there was a multipoly.. make it so that
            # doesn't happen and you won't have to worry about this, dummy.
            print(e)
            print(coord)
            sys.exit(0)
        new_coords = [(p.x, p.y) for p in new_shape['points']]
        new_shape['poly'] = Polygon(new_coords)
        converted_shapes.append(new_shape)

    # initialize this shape's neighbor list to an empty list
    neighbors_dict[current_shape['properties']['GEOID10']] = {}
    neighbors_dict[current_shape['properties']['GEOID10']]['neighbors'] = []

    try:
        current_shape = next(shape_iter)
    except StopIteration as e:
        break
print("Converting polygons to X, Y coordinates DONE.")

print("\nConstructing shapes list...")
polys = []
for shape in converted_shapes:
    polys.append(shape['poly'])

# make a dictionary so that we can lookup the objects in the tree, so we can
# tell which polygon is which
print("Creating ID mapping...")
index_by_id = dict((id(poly), idx) for idx, poly in enumerate(polys))

# construct the tree and iterate over each shape, expanding it by 200' and
# getting the list of all shapes the expanded one overlaps
print("Constructing STR Tree...")
tree = STRtree(polys)
for shape in converted_shapes:
    buffered_poly = shape['poly'].buffer(BUFFER_SIZE)
    for intersected_poly in tree.query(buffered_poly):
        # make sure we're not checking against ourselves...
        if shape['geoid'] != converted_shapes[index_by_id[id(intersected_poly)]]['geoid']:
            # check for at least 200' in common
            print(f"Checking shared border length of {shape['geoid']} and {converted_shapes[index_by_id[id(intersected_poly)]]['geoid']}... ", end='')
            length = shape['poly'].intersection(intersected_poly).length * KM_TO_FEET
            if length >= 200:
                # all good, save it as a neighbor
                print(f"OK ({length} ft)")
                if converted_shapes[index_by_id[id(intersected_poly)]]['geoid'] not in neighbors_dict[shape['geoid']]['neighbors']:
                    neighbors_dict[shape['geoid']]['neighbors'].append(converted_shapes[index_by_id[id(intersected_poly)]]['geoid'])
            else:
                print(f"FAIL ({length} ft)")

    # this is for florida, which has 10 teeny tiny precincts which don't have
    # a 200ft border themselves for some reason, so there's absolutely no way
    # they can share a border of 200ft with another precinct.
    if len(neighbors_dict[shape['geoid']]['neighbors']) == 0:
        print(f"{shape['geoid']} has no neighbors! adding all touching precincts")
        for intersected_poly in tree.query(shape['poly']):
            if shape['geoid'] == converted_shapes[index_by_id[id(intersected_poly)]]['geoid']:
                continue

            if not shape['poly'].touches(intersected_poly) and not shape['poly'].within(intersected_poly):
                continue

            if converted_shapes[index_by_id[id(intersected_poly)]]['geoid'] not in neighbors_dict[shape['geoid']]['neighbors']:
                print(f"... adding {converted_shapes[index_by_id[id(intersected_poly)]]['geoid']} as a neighbor")
                neighbors_dict[shape['geoid']]['neighbors'].append(converted_shapes[index_by_id[id(intersected_poly)]]['geoid'])
                neighbors_dict[converted_shapes[index_by_id[id(intersected_poly)]]['geoid']]['neighbors'].append(shape['geoid'])

print(f"Writing output to {outfile}...")
with open(outfile, 'w') as f:
    json.dump(neighbors_dict, f, indent=2)
print(f"Wrote {len(neighbors_dict)} records.")
