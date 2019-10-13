from collections import defaultdict


class Annotation(object):
    """This class represents an annotation."""

    def __init__(self, id, representation, spans, labels=()):
        """
        Create an annotation object.

        :param id: (string) The id of the current annotation.
        :param representation: (string) The string representation of the
        annotation. Doesn't take into account the fact that annotations may be
        discontinous.
        :param spans: (list of list of ints) A list of list of ints
        representing the starting and ending points, in characters, for any
        words in the annotation.
        :param labels: (list of strings) a list of initial labels for the
        annotation object. These never get an initial value.
        :return: None
        """
        self.id = id
        self.links = defaultdict(list)
        self.labels = defaultdict(list)
        for label in labels:
            self.labels[label] = []
        self.label = list(self.labels.keys())[0]
        self.repr = representation
        self.spans = spans
        self.realspan = (spans[0][0], spans[-1][1])
        self.words = []
        self.type = []
        self.args = []

    def __repr__(self):        
        """Representation of the Annotation."""
        temp_ann = 'Annotation:' 
        temp_ann = temp_ann + 'id:' + str(self.id)
        temp_ann = temp_ann + '\ttype:' + str(self.type)
        temp_ann = temp_ann + '\trepr:' + self.repr
        temp_ann = temp_ann + '\tlabels:' + str(list(self.labels.keys()))
        temp_ann = temp_ann + '\tspans:' + str(self.spans)
        #temp_ann = temp_ann + '\trealspan:' + str(self.realspan)
        #temp_ann = temp_ann + '\tlabel:' + list(self.labels.keys())[0]
        temp_ann = temp_ann + '\targs:' + str(self.args)
        #temp_ann = temp_ann + '\nlinks:' + str(list(self.links))
        #temp_ann = temp_ann + '\nWords:\n'
        #ind = 0
        #for word in self.words:
        #    temp_ann = temp_ann + 'words[' + str(ind)+ ']:' + str(word)
        #    ind = ind + 1
        return "{0}".format(temp_ann)
    
