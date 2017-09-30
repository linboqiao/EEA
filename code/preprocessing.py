from nltk.stem.porter import *
import sys,json
from collections import defaultdict

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

	def createJson(self, in1, in2, in3, out):
		with open(in1) as IN1, open(in2) as IN2, open(in3) as IN3, open(out, 'w') as OUT:
			for l1,l2,l3 in zip(IN1, IN2, IN3):
				tmp = dict()
				tmp['tokens'] = l1.strip().split(' ')
				tmp['pos'] = l3.strip().split(' ')
				assert(len(tmp['tokens']) == len(tmp['pos']))
				tmp['entityMentions'] = []
				bio = l2.strip().split(' ')
				assert(len(tmp['tokens']) == len(bio))
				begin = False
				start = None
				type_ = 'O'
				for index, i in enumerate(bio):
					if i != 'O' and (i == type_ or not begin):
						if begin == False:
							begin = True
							start = index
							type_ = i
					elif begin == True:
						tmp['entityMentions'].append((start, index, type_))
						type_ = i
						if i == 'O':
							begin = False
						else:
							start = index
				OUT.write(json.dumps(tmp)+'\n')
				
				#break

	def relationLinker(self, file_path):
		relation_token=set(["VB","VBD","VBG","VBN","VBP","VBZ"])
		V_pattern = "((VB|VBD|VBG|VBN|VBN|VBP|VBZ) )+"
		P_pattern = "((IN|RP) ?)"
		W_pattern = "((NN.{0,2}|JJ.{0,1}|RB.{0,1}|PRP.{0,1}|DT ))+"
		relation_pattern = V_pattern+W_pattern+P_pattern+"|"+V_pattern+P_pattern+"|"+V_pattern
		print(relation_pattern)
		matched_phrases=defaultdict(int)
		#relation_pattern=set()
		#references=[]
		'''
		with open(postag_path) as IN:
			for line in IN:
				if ' ' in line:
					relation_pattern.add(line.strip())
		for i in list(relation_pattern):
			references.append(i.split(' '))
		'''

		with open(file_path,'r') as IN:
			cnt=0
			for line in IN:
				#if cnt > 100:
				#	break
				cnt+=1
				#print line
				tmp=json.loads(line)

				for i,e in enumerate(tmp['entityMentions']):
					for j in range(i+1, len(tmp['entityMentions'])):
						if tmp['entityMentions'][j][0]-e[1] >= 0:
							candidate = ' '.join(tmp['pos'][e[1]:tmp['entityMentions'][j][0]])
							m = re.search(relation_pattern, candidate)
							if m:
								result=m.group(0).rstrip()
								length = result.count(' ')
								#print length
								index=candidate.find(m.group(0))
								#print candidate,index
								index = candidate[:index].count(' ')
								#print candidate
								#print index,m.group(0)
								relations=' '.join(tmp['tokens'][e[1]:tmp['entityMentions'][j][0]][index:index+length+1])
								matched_phrases[relations]+=1
								#matched_unigram[result]+=1
						#if tmp['entity_mentions'][i+1]['start']-e['end'] >= length:
						'''
							for idx in xrange(e[1],tmp['entity_mentions'][i+1][0]):
								if tmp['pos'][idx:idx+length] == r:
									#print r,tmp['tokens'][idx:idx+length]
									matched_phrases[' '.join(tmp['tokens'][idx:idx+length])]+=1
									#print ' '.join(tmp['tokens'][idx:idx+length])
								if tmp['pos'][idx] in relation_token:
									matched_unigram[tmp['tokens'][idx]]+=1
						'''
			for k,v in matched_phrases.items():
				if v > 9:
					print(k)


if __name__ == '__main__':
	tmp = Preprocessor('')
	#tmp.readfile(sys.argv[1], sys.argv[2])
	#tmp.createJson(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
	print(sys.argv)
	tmp.relationLinker(sys.argv[1])
