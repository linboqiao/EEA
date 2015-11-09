#!/usr/bin/python

import sys

f = open(sys.argv[1])
f_out = open(sys.argv[2], 'w')

doc_id = ""

clusters = {}

for line in f:
    if line.startswith("#"):
        if line.startswith("#BeginOf"):
            doc_id = line.strip().split()[1]
        if line.startswith("#EndOf"):
            used_tokens = set()
            for index, ((type, realis), tokens2Ids) in enumerate(clusters.iteritems()):
                if len(tokens2Ids) > 1:
                    f_out.write("@Coreference\tR%d\t%s\n" % (index, ",".join(tokens2Ids.values())))
            clusters = {}
    else:
        fields = line.split()
        type = fields[5]
        realis = fields[6]
        eid = fields[2]
        tokens = fields[3]
        try:
            clusters[(type, realis)][tokens] = eid
        except KeyError:
            clusters[(type, realis)] = {tokens: eid}
            pass
    f_out.write(line)
