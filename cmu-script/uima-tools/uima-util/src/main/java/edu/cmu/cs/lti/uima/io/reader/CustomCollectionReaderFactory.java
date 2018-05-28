package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.CrossValidationReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class CustomCollectionReaderFactory {
    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(String parentInputDirName,
                                                              String baseInputDirName,
                                                              Integer stepNumber,
                                                              Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
                StepBasedDirXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Creates a simple XMI reader with the specified type system, assuming the directory naming
     * convention.
     *
     * @param typeSystemDescription
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(
            TypeSystemDescription typeSystemDescription, String inputDirName,
            Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription,
                XmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                XmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription, String
            parentInputDirName, String baseInputDirName, Integer stepNumber)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(String parentInputDirName, String baseInputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription, String
            parentInputDirName, String baseInputDirName) throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName
        );
    }


    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRandomizedXmiReader(TypeSystemDescription typeSystemDescription,
                                                                        String parentInputDirName,
                                                                        String baseInputDirName,
                                                                        int randomSeed)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                RandomizedXmiCollectionReader.class, typeSystemDescription,
                RandomizedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                RandomizedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                RandomizedXmiCollectionReader.PARAM_SEED, randomSeed
        );
    }

    /**
     * Create a cross validation reader
     *
     * @param typeSystemDescription The type system of the cas.
     * @param parentInputDirName    The main working directory.
     * @param baseInputDirName      The directory under parentInput that really contains the data.
     * @param takeEvalSplit         Whether to take the evaluation split (small slice).
     * @param randomSeed            The random seed.
     * @param slice                 Choose which slice as eval (others as training)
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createCrossValidationReader(TypeSystemDescription typeSystemDescription,
                                                                          String parentInputDirName,
                                                                          String baseInputDirName,
                                                                          boolean takeEvalSplit,
                                                                          int randomSeed,
                                                                          int split,
                                                                          int slice)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                CrossValidationReader.class, typeSystemDescription,
                CrossValidationReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                CrossValidationReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                CrossValidationReader.PARAM_MODE_EVAL, takeEvalSplit,
                CrossValidationReader.PARAM_SEED, randomSeed,
                CrossValidationReader.PARAM_SLICE, slice,
                CrossValidationReader.PARAM_SPLITS, split
        );
    }

    /**
     * Create a cross validation reader
     *
     * @param typeSystemDescription The type system of the cas.
     * @param parentInputDirName    The main working directory.
     * @param baseInputDirName      The directory under parentInput that really contains the data.
     * @param extension             The file extension to read (no ".")
     * @param takeEvalSplit         Whether to take the evaluation split (small slice).
     * @param randomSeed            The random seed.
     * @param slice                 Choose which slice as eval (others as training)
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createCrossValidationReader(TypeSystemDescription typeSystemDescription,
                                                                          String parentInputDirName,
                                                                          String baseInputDirName,
                                                                          String extension,
                                                                          boolean takeEvalSplit,
                                                                          int randomSeed,
                                                                          int slice)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                CrossValidationReader.class, typeSystemDescription,
                CrossValidationReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                CrossValidationReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                CrossValidationReader.PARAM_MODE_EVAL, takeEvalSplit,
                CrossValidationReader.PARAM_SEED, randomSeed,
                CrossValidationReader.PARAM_SLICE, slice,
                AbstractStepBasedDirReader.PARAM_INPUT_FILE_SUFFIX, extension
        );
    }


    /**
     * Create a xmi reader that read the documents according to the timestamp (Gigaword docs)
     *
     * @param typeSystemDescription
     * @param parentInputDir
     * @param baseInputDir
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createTimeSortedXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDir, String baseInputDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                TimeSortedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDir,
                TimeSortedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDir
        );
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(TypeSystemDescription typeSystemDescription,
                                                                     String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRecursiveGzippedXmiReader(TypeSystemDescription
                                                                                      typeSystemDescription, String
                                                                                      inputDirName, Boolean
                                                                                      failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true);
    }


    /**
     * Create a gzipped XMI reader, with specified type system. Assuming the directory naming
     * convention.
     *
     * @param typeSystemDescription
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createStepBasedGzippedXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDirName,
            String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirGzippedXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }


    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createStepBasedGzippedXmiReader(String parentInputDirName,
                                                                              String baseInputDirName, Integer
                                                                                      stepNumber)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirGzippedXmiCollectionReader.class,
                StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber
        );
    }

    public static CollectionReaderDescription createTimeSortedGzipXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDir, String baseInputDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                TimeSortedGzippedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDir,
                TimeSortedGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDir
        );
    }
}
