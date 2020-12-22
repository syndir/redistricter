#!/usr/bin/env python3
#
# This script just takes 1 JSON file, reads it in, and dumps it back out to
# another JSON file without any linebreaks. This is done to shrink the file.

import json
import sys


if len(sys.argv) != 3:
    print(f"{sys.argv[0]} [infile] [outfile]")
    sys.exit(0)

with open(sys.argv[1], 'r') as f:
    data = json.load(f)

with open(sys.argv[2], 'w') as f:
    json.dump(data, f)
