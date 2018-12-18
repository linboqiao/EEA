"""
Convert tbf output to Central Semantic Repository JSON style file.
"""
import json
import os

def read_file(tbf):
    with open(tbf) as input:
        docname = ""
        records = {}
        relations = []
        for line in input:
            line = line.strip()
            if line.startswith("#BeginOfDocument"):
                relations = []
                records = {}
                docname = line.split()[1]
            elif line.startswith("#EndOfDocument"):
                yield docname, records, relations
            elif line.startswith("@"):
                relations.append(line.split("\t"))
            else:
                parts = line.split("\t")
                records[parts[2]] = parts


def main(tbf, output):
    tbf = os.path.abspath(tbf)
    print("Writing %s to JSON at %s" % (tbf, output))

    group_id = 'cmu_event'
    l_csr = []
    for docname, records, relations in read_file(tbf):
        csr = {
            'id': "%s:%s_hector_events" % (group_id, docname),
            'sourceID': tbf + "_" + docname,
            'raw_input': docname,
            'interp': []
        }

        for eid, record in records.items():
            model_name, _, eid, span, surface, type, realis = record[:7]

            span = [int(s) for s in span.split(',')]

            mention = {
                'record_type': 'text_mention',
                'model': model_name,
                'id': eid,
                'char_span': span,
                'surface': surface,
                'mention_type': type,
                'realis': realis,
            }

            csr['interp'].append(mention)

        for relation in relations:
            relation_type, id, args = relation

            args = args.split(',')

            relation = {
                'record_type': 'mention_relation',
                'relation_type': relation_type,
                'members': args,
                'id': id
            }

            csr['interp'].append(relation)
        l_csr.append(csr)

    with open(output, 'w') as out:
        json.dump(l_csr, out, indent=2)


if __name__ == '__main__':
    import sys

    input = sys.argv[1]
    output = sys.argv[2]
    main(input, output)
