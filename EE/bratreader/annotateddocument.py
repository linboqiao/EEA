from itertools import chain
from lxml import etree


class AnnotatedDocument(object):
    """Represent a document in a Brat Corpus."""

    def __init__(self, key, sentences):
        """
        Create a brat document.

        :param key: (string) The key of the document.
        Generally the name of the file without the extension
        (e.g. "022.ann" becomes 022)
        :param sentences: A list of dictionaries containing words.
        Represents the text of the review on a word-by-word basis.
        :return: None
        """
        self.sentences = sentences
        annotations = [chain.from_iterable([w.annotations for w in x.words])
                       for x in sentences]
        self.annotations = list(chain.from_iterable(annotations))

        self.key = key

        self.text = u"\n".join([u" ".join([x.form for x in s.words])
                               for s in sentences])            
                          
    def __repr__(self):
        """Representation of the AnnotatedDocument."""
        temp_ann = 'AnnotatedDocument:'
        # elements in sentences
        temp_ann = temp_ann + '\n\nkey:\n' + str(self.key)
        temp_ann = temp_ann + '\n\ntext:\n' + self.text
        temp_ann = temp_ann + '\n\nSentences:'
        ind = 0
        for sent in self.sentences:
            temp_ann = temp_ann + '\nsent[' + str(ind) + ']:\n' + str(sent)
            ind = ind + 1
        temp_ann = temp_ann + '\n\nAnnotations:'
        ind = 0
        for ann in self.annotations:
            temp_ann = temp_ann + '\nann[' + str(ind) + ']:\n' + str(ann)
            ind = ind + 1
        return "{0}\n".format(temp_ann)
    
        
    def getlabelinspan(self, start, end):
        """
        Retrieve all labels in the specified character span.

        :param start: The start index in characters.
        :param end: The end index in characters.
        :return a list of labels that fall inside the span.
        """
        return [list(ann.labels.keys())[0] for ann in self.annotations if 
                (ann.spans[0][0] <= start < ann.spans[-1][1])
                or (ann.spans[0][0] < end <= ann.spans[-1][1])
                or (start < ann.spans[0][0] < end and start < ann.spans[-1][1] < end)]
    
    

    def export_xml(self, pathtofile):
        """
        Export the current document to an XML file at the specified location.

        :param pathtofile: The path where the .XML file needs to be saved.
        :return: None
        """
        document = etree.Element("document", source=self.key)

        sentences = etree.Element("sentences")
        for s in self.sentences:

            sentence = etree.Element("sentence", id="s.{0}".format(s.key),
                                     start=str(s.start),
                                     end=str(s.end))

            for w in s.words:

                word = etree.Element("word",
                                     start=str(w.start),
                                     end=str(w.end),
                                     id="s.{0}.w.{1}".format(w.sentkey, w.key))
                word.text = w.form
                sentence.append(word)

            sentences.append(sentence)

        document.append(sentences)

        annotations = etree.Element("annotations")

        for v in self.annotations:

            annotations.append(etree.Element("annotation",
                                             id=str("ann{0}".format(v.id))))
            ann = annotations.getchildren()[-1]

            ann.set("words", u" ".join(["s.{0}.w.{1}".format(w.sentkey, w.key)
                                       for w in v.words]))
            ann.set("repr", v.repr)
            ann.set("spans", u",".join(["|".join([str(y) for y in x])
                                      for x in v.spans]))

            for label, valency in v.labels.items():
                ann.set(str(label), "|".join(valency))

            for linktype, linked in v.links.items():

                linked = u" ".join(["ann{0}".format(link.id) for link in linked])
                ann.set(str("link.{0}".format(linktype)), linked)

        document.append(annotations)

        with open(pathtofile, 'wb') as f:
            etree.ElementTree(document).write(f, pretty_print=True)
