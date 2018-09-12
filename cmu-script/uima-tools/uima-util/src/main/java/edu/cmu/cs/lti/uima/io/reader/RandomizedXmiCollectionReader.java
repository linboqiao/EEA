package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class RandomizedXmiCollectionReader extends AbstractStepBasedDirReader {
    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    public static final String PARAM_SEED = "seed";
    @ConfigurationParameter(name = PARAM_SEED, defaultValue = "17")
    private int initialSeed;

    private String inputViewName;

    private List<File> xmiFiles;

    private int currentDocIndex;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        logger.info("Reading data with random seed " + initialSeed);

        inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = ".xmi";
        }

        // Get a list of XMI files in the specified directory
        xmiFiles = new ArrayList<>();
        File[] files = inputDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && files[i].getName().endsWith(inputFileSuffix)) {
                xmiFiles.add(files[i]);
            }
        }

        Collections.sort(xmiFiles);
        Collections.shuffle(xmiFiles, new Random(initialSeed));

        currentDocIndex = 0;
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    public boolean hasNext() {
        return currentDocIndex < xmiFiles.size();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     */
    public void getNext(JCas jCas) throws IOException, CollectionException {
        try {
            if (!StringUtils.isEmpty(inputViewName)) {
                jCas = jCas.getView(inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        CasSerialization.readXmi(jCas, xmiFiles.get(currentDocIndex));
        currentDocIndex++;
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() throws IOException {
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, xmiFiles.size(), Progress.ENTITIES)};
    }


}