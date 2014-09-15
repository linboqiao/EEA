package edu.cmu.cs.lti.cr.readers.annotated_nyt;

import com.nytlabs.corpus.NYTCorpusDocument;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 8/27/14
 * Time: 9:23 PM
 */
public class SubcorpusCreator {
    public static void printUsage(){
        System.out.println("Usage: SubcorpusCreator [input directory] [output directory] [category path]");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length < 3){
            printUsage();
        }

        String inputDir = args[0];
        String outDir = args[1];
        String categoryPath = args[2];

        Set<String> catogories = new HashSet<String>(IOUtils.readLines(new FileInputStream(new File(categoryPath))));

        AnnotatedNytReader reader = new AnnotatedNytReader(new File(inputDir));

        reader.readAll();

        while (reader.hasNextDocument()) {
            NYTCorpusDocument doc = reader.getNextDocument();
            List<String> descriptors = doc.getTaxonomicClassifiers();

            for (String des : descriptors){
                for (String sDes : des.split("/")){
                    if (catogories.contains(sDes.toLowerCase())) {
                        reader.dumpDocumentByDate(new File(outDir));
                    }
                }
            }
        }
    }
}
