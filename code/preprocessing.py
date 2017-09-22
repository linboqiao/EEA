from nltk.stem.porter import *
import sys

class Preprocessor(object):
	"""docstring for Preprocessor"""
	def __init__(self, arg):
		self.arg = arg
		self.stemmer = PorterStemmer()

	def preprocess(self, sentence):
		output = []
		for item in sentence:
			output.append(self.stemmer.stem(item.lower()))
		return ' '.join(output)

	def readfile(self, in1, out1):
		with open(in1) as IN, open(out1, 'w') as OUT:
			for line in IN:
				OUT.write(self.preprocess(line.split(' ')))

if __name__ == '__main__':
	tmp = Preprocessor('')
	tmp.readfile(sys.argv[1], sys.argv[2])