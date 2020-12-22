#!/usr/bin/env python3
#
# Used to verify neighbors file.
# Checks that any given entry should have a returning edge.


import sys
import json
from collections import defaultdict

if len(sys.argv) != 2:
    print(f"{sys.argv[0]} [neighbors file to check]")
    sys.exit(0)

neighbors_file = sys.argv[1]

neighbors_dict = defaultdict(dict)
with open(neighbors_file, 'r') as f:
    data = json.load(f)
    for neighbor in data:
        if len(data[neighbor]['neighbors']) == 0:
            print(f"{neighbor} has no neighbors!")
        for adj in data[neighbor]['neighbors']:
            neighbors_dict[neighbor][adj] = 1
            if adj in neighbors_dict:
                try:
                    temp = neighbors_dict[adj][neighbor]
                except Exception:
                    print(f"Missing returning edge: {neighbor}->{adj}")
