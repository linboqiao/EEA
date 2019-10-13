class Word(object):

    def __init__(self, key, sentkey, form, start, end):
        """
        Define a word object.

        :param key: The key of the document to which this belongs.
        :param sentkey: The key of the sentence to which this word belongs.
        :param form: The string form of this word.
        :param start: The start index of this word.
        :param end: The end index of this word.
        """
        self.key = key
        self.sentkey = sentkey
        self.form = form
        self.start = start
        self.end = end
        self.annotations = []  

    def __repr__(self):
        """Representation of the Sentence."""
        temp_ann = 'Word:'
        # elements in sentences
        temp_ann = temp_ann + 'key:' + str(self.key)
        temp_ann = temp_ann + '\tsentkey:' + str(self.sentkey)
        temp_ann = temp_ann + '\tstart:' + str(self.start)
        temp_ann = temp_ann + '\tend:' + str(self.end)
        temp_ann = temp_ann + '\tform:' + str(self.form.strip())
        temp_ann = temp_ann + '\nannotations:'
        ind = 0
        for ann in self.annotations:
            temp_ann = temp_ann + '\t' + ann.repr + ',labels:' + str(list(ann.labels.keys()))
            ind = ind + 1
        return "{0}".format(temp_ann)
    
