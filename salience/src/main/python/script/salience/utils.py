"""
Various utilities
"""


def read_salience_file(path):
    start = True
    with open(path) as f:
        data = {}
        for line in f:
            line = line.strip()
            if line == "":
                start = True
                yield data
                data = {}
                continue

            if start:
                docid, title = line.split(" ", 1)
                start = False
                data['docid'] = docid
                data['title'] = title
                data['entities'] = []
            else:
                parts = line.split('\t')
                # Some files may have additional appended, which are ignored for now.
                index, salience, count, surface, begin, end, kbid = parts[0:7]
                data['entities'].append((int(index), int(salience), count, surface, int(begin), int(end), kbid))
