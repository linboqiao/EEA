package edu.cmu.cs.lti.uima.annotator;

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.uima.io.reader.AbstractStepBasedDirReader;
import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 10:27 AM
 *
 * @author Zhengzhong Liu
 */
public class CrossValidationReader extends AbstractStepBasedDirReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());


    public static final String PARAM_SEED = "seed";
    @ConfigurationParameter(name = PARAM_SEED, defaultValue = "17")
    private int seed;
    public static final String PARAM_SPLITS = "splits";
    @ConfigurationParameter(name = PARAM_SPLITS, defaultValue = "5")
    private int splitsCnt;
    public static final String PARAM_SLICE = "slice";
    @ConfigurationParameter(name = PARAM_SLICE, description = "which slice of the above split")
    private int slice;
    public static final String PARAM_MODE_EVAL = "modeEval";
    @ConfigurationParameter(name = PARAM_MODE_EVAL, description = "true => eval (=returns 1 slice for eval);"
            + " false => train (=returns e.g. 9 slices for training)")
    private boolean modeEval;

    private static String DEFAULT_SUFFIX = "xmi";

    private List<File> corpus;

    private int currentIndex;
    private Iterator<File> corpusIter;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        logger.info("Starting cross validation reader for " + (modeEval ? "evaluation" : "training"));

        if (inputFileSuffix == null) {
            inputFileSuffix = DEFAULT_SUFFIX;
        }

        List<File> files = new ArrayList<>(FileUtils.listFiles(inputDir, new String[]{inputFileSuffix}, false));

        if (files.size() < splitsCnt) {
            throw new IllegalArgumentException(String.format("Number of files [%d] smaller than split count [%d].",
                    files.size(), splitsCnt));
        }

        // Always sorted to ensure the split are the same at different place.
        Collections.sort(files);
        Collections.shuffle(files, new Random(seed));
        int splitSize = (int) Math.ceil(files.size() / splitsCnt);
        List<List<File>> partitions = Lists.partition(files, splitSize);


        if (modeEval) {
            corpus = partitions.get(slice);
            logger.info(String.format("Reading %d files for development.", corpus.size()));
        } else {
            corpus = new ArrayList<>();
            for (int i = 0; i < partitions.size(); i++) {
                if (i != slice) {
                    corpus.addAll(partitions.get(i));
                }
            }
            logger.info(String.format("Reading %d files for training.", corpus.size()));
        }
        corpusIter = corpus.iterator();
        currentIndex = 0;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        JCas inputView = JCasUtil.getView(jCas, inputViewName, jCas);
        currentIndex++;
        CasSerialization.readXmi(inputView, corpusIter.next());
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return corpusIter.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentIndex, corpus.size(), Progress.ENTITIES)};
    }
}
