#!/usr/bin/env python3
#
# Computes the population of a VTD based upon the blockgroups it contains
#
# Block Assignment File format:
# BLOCKID, COUNTYFP, DISTRICT
# where DISTRICT is the VTD that BLOCKID resides in
#
# CVAP File:
# BLOCKGRP, DEMO, T_POP, V_POP
# BLOCKGRP -> identifier of the block group, which may span >1 VTD
# DEMO -> id of the demographic, 1-13 (5: AFR_AM, 4: AS_AM, 13: HISP, 11:
#         AM_IN). The rest go into "other"
# T_POP -> Total pop of demographic
# V_POP -> VAP of demographic

import sys
import csv
import json


if len(sys.argv) != 5:
    print(f"{sys.argv[0]} [blockgroup file] [cvap file] [json file to join with] [output file]")
    sys.exit(0)

blockgrp_file = sys.argv[1]
cvap_file = sys.argv[2]
json_file = sys.argv[3]
output_file = sys.argv[4]

counties = {}

# process the blockgroup (vtd designation) file
with open(blockgrp_file, 'r') as bg_file:
    csv_file = csv.DictReader(bg_file)
    for row in csv_file:
        countyid = row['COUNTYFP']
        blockid = row['BLOCKID'][0:-3]

        if not counties.get(countyid):
            counties[countyid] = {}

        if counties[countyid].get(blockid):
            if row['DISTRICT'] not in counties[countyid][blockid]['dist_list']:
                counties[countyid][blockid]['dist_list'].append(row['DISTRICT'])
                counties[countyid][blockid]['num_districts'] += 1
        else:
            counties[countyid][blockid] = {}
            counties[countyid][blockid]['num_districts'] = 1
            counties[countyid][blockid]['dist_list'] = [ row['DISTRICT'] ]


# now make the VTD dict and initialize values to 0
for county in counties:
    for block in counties[county]:
        for district in counties[county][block]['dist_list']:
            counties[county][block][district] = {}
            counties[county][block][district]['countyname'] = ""
            counties[county][block][district]['t_aa'] = 0
            counties[county][block][district]['v_aa'] = 0
            counties[county][block][district]['t_as'] = 0
            counties[county][block][district]['v_as'] = 0
            counties[county][block][district]['t_hisp'] = 0
            counties[county][block][district]['v_hisp'] = 0
            counties[county][block][district]['t_ai'] = 0
            counties[county][block][district]['v_ai'] = 0
            counties[county][block][district]['t_oth'] = 0
            counties[county][block][district]['v_oth'] = 0
            counties[county][block][district]['t_pop'] = 0
            counties[county][block][district]['v_pop'] = 0

# process the cvap file
with open(cvap_file, 'r') as pop_file:
    csv_file = csv.DictReader(pop_file)
    for row in csv_file:
        blockgrp = row['BLOCKGRP'][7:]
        countyid = row['BLOCKGRP'][9:12]
        county = row['COUNTY'].split(',')[2].strip()

        if counties[countyid].get(blockgrp):
            for district in counties[countyid][blockgrp]['dist_list']:
                counties[countyid][blockgrp][district]['countyname'] = county
                if row['DEMO'] == '5': # AFR_AM
                    counties[countyid][blockgrp][district]['t_aa'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_aa'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
                elif row['DEMO'] == '4': # AS_AM
                    counties[countyid][blockgrp][district]['t_as'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_as'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
                elif row['DEMO'] == '13': # HISP
                    counties[countyid][blockgrp][district]['t_hisp'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_hisp'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
                elif row['DEMO'] == '11': # AI
                    counties[countyid][blockgrp][district]['t_ai'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_ai'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
                elif row['DEMO'] == '1': # TOTAL
                    counties[countyid][blockgrp][district]['t_pop'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_pop'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
                elif row['DEMO'] != '2': # TOTAL NON-HISP.. should be ignored (double count)
                    counties[countyid][blockgrp][district]['t_oth'] += \
                            (int(row['T_POP']) // counties[countyid][blockgrp]['num_districts'])
                    counties[countyid][blockgrp][district]['v_oth'] += \
                            (int(row['V_POP']) // counties[countyid][blockgrp]['num_districts'])
        else:
            print(f"{blockgrp} not found in map!")


# load the json file we want to join with
data = None
with open(json_file, 'r') as f:
    data = json.load(f)

for feature in data['features']:

    # initialize fields to 0
    feature['properties']['T_AA'] = 0
    feature['properties']['V_AA'] = 0
    feature['properties']['T_AS'] = 0
    feature['properties']['V_AS'] = 0
    feature['properties']['T_HISP'] = 0
    feature['properties']['V_HISP'] = 0
    feature['properties']['T_AI'] = 0
    feature['properties']['V_AI'] = 0
    feature['properties']['T_OTH'] = 0
    feature['properties']['V_OTH'] = 0
    feature['properties']['T_POP'] = 0
    feature['properties']['V_POP'] = 0

    county_tag = feature['properties']['COUNTYFP10']
    precinct_tag = feature['properties']['VTDST10']

    for blockgrp in counties[county_tag]:
        if counties[county_tag][blockgrp].get(precinct_tag):
            precinct = counties[county_tag][blockgrp][precinct_tag]
            feature['properties']['COUNTY'] = precinct['countyname']
            feature['properties']['T_AA'] += precinct['t_aa']
            feature['properties']['V_AA'] += precinct['v_aa']
            feature['properties']['T_AS'] += precinct['t_as']
            feature['properties']['V_AS'] += precinct['v_as']
            feature['properties']['T_HISP'] += precinct['t_hisp']
            feature['properties']['V_HISP'] += precinct['v_hisp']
            feature['properties']['T_AI'] += precinct['t_ai']
            feature['properties']['V_AI'] += precinct['v_ai']
            feature['properties']['T_OTH'] += precinct['t_oth']
            feature['properties']['V_OTH'] += precinct['v_oth']
            feature['properties']['T_POP'] += precinct['t_pop']
            feature['properties']['V_POP'] += precinct['v_pop']

            # After we've added it, we can delete it from the dictionary so
            # we don't have to keep iterating over things we've already updated
            counties[county_tag][blockgrp].pop(precinct_tag)


with open(output_file, 'w') as f:
    # json.dump(data, f, indent=2)
    json.dump(data, f)
