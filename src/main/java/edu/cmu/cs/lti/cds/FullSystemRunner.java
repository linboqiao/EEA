package edu.cmu.cs.lti.cds;

import edu.cmu.cs.lti.cr.readers.annotated_nyt.AnnotatedNytReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FullSystemRunner {

    public static void main(String[] args) {
        String dataDir = args[0];

        AnnotatedNytReader reader = new AnnotatedNytReader(new File(dataDir));

        RelatedDocumentFinder finder = new RelatedDocumentFinder();

        while (reader.hasNextDay()){
            reader.readNextDay();
            List<String> docOfSameDay = new ArrayList<String>();
            while (reader.hasNextDocument()){
                docOfSameDay.add(reader.getNextDocument().getBody());
            }

            List<List<String>> relatedDocuments = finder.findDocumentsByEntity(docOfSameDay);

            for (String docStr : relatedDocuments.get(0)){
                System.out.println(docStr);
            }

            break;//test on the first day
        }


    }
}