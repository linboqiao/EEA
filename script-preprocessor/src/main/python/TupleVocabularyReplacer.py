#!/usr/bin/python
import glob
import os
from collections import Counter

import sys

# preposition_size = 50
# noun_size = 8000
# verb_size = 2000

preposition_size = 50
noun_size = 8000
verb_size = 2000

tuple_out_dir = sys.argv[1]
tuple_files = sys.argv[2:]

missing_symbol = "-"
oov_symbol = "<OOV>"

print "Number of input files: %d" % len(tuple_files)
print "Output directory is: ", tuple_out_dir

class Vocabulary:
    def __init__(self, size):
        self.size = size
        self.vocab = Counter()

    def add(self, word):
        self.vocab[word] += 1

    def get_most_common(self):
        most_common = set()
        for w, _ in self.vocab.most_common(self.size):
            most_common.add(w)
        return most_common

    def write_most_common(self, out_file):
        with open(out_file, 'w') as out:
            for word, count in self.vocab.most_common(self.size):
                out.write("%s\t%d\n" % (word, count))

noun_vocab = Vocabulary(noun_size)
prep_vocab = Vocabulary(preposition_size)
verb_vocab = Vocabulary(verb_size)

for filepath in tuple_files:
    with open(filepath) as tuple_file:
        i = 0

        for line in tuple_file:
            print filepath, line, str(i)
            i+=1

            fields = line.strip().split("\t")

            if len(fields) >= 7:
                subj, verb, dobj, iobj = fields[:4]

                if not subj == missing_symbol:
                    noun_vocab.add(subj)

                if not dobj == missing_symbol:
                    noun_vocab.add(dobj)

                if not iobj == missing_symbol:
                    noun_vocab.add(iobj)

                for v in verb.split("_"):
                    verb_vocab.add(v)

            if len(fields) > 9:
                prep_words = fields[8:]
                for prep, noun, noun_id in [prep_words[i: i + 3] for i in range(0, len(prep_words), 3)]:
                    noun_vocab.add(noun)
                    prep_vocab.add(prep)

if not os.path.isdir(tuple_out_dir):
    os.makedirs(tuple_out_dir)

noun_vocab.write_most_common(os.path.join(tuple_out_dir, "noun_vocab.txt"))
prep_vocab.write_most_common(os.path.join(tuple_out_dir, "prep_vocab.txt"))
verb_vocab.write_most_common(os.path.join(tuple_out_dir, "verb_vocab.txt"))

common_nouns = noun_vocab.get_most_common()
common_preps = prep_vocab.get_most_common()
common_verbs = verb_vocab.get_most_common()

for filepath in glob.glob(tuple_files):
    out_path = os.path.join(tuple_out_dir, os.path.basename(filepath))

    with open(filepath) as tuple_file, open(out_path, 'w') as out_file:
        new_fields = []

        for line in tuple_file:
            fields = line.strip().split("\t")

            i += 1

            if len(fields) >= 7:
                subj, verb, dobj, iobj = fields[:4]

                if not subj == missing_symbol:
                    if subj not in common_nouns:
                        subj = oov_symbol

                if not dobj == missing_symbol:
                    if dobj not in common_nouns:
                        dobj = oov_symbol

                if not iobj == missing_symbol:
                    if iobj not in common_nouns:
                        iobj = oov_symbol

                verb_fixed = ""
                sep = ""
                for v in verb.split("_"):
                    verb_fixed += sep

                    if v in common_verbs:
                        verb_fixed += v
                    else:
                        verb_fixed += oov_symbol

                    sep = "_"

                new_fields = [subj, verb_fixed, dobj, iobj] + fields[4:8]

            if len(fields) > 9:
                prep_words = fields[8:]
                for prep, noun, noun_id in [prep_words[i: i + 3] for i in range(0, len(prep_words), 3)]:
                    if prep in common_preps:
                        new_fields.append(prep)
                    else:
                        new_fields.append(oov_symbol)

                    if noun in common_nouns:
                        new_fields.append(noun)
                    else:
                        new_fields.append(oov_symbol)

                    new_fields.append(noun_id)

            if len(fields) == 1:
                out_file.write(line)
            else:
                out_file.write("\t".join(new_fields) + "\n")
