package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ModelTester {
    final private String middleResults;
    private final boolean charOffset;

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    //    private final String evalLogOutputDir;
    private final String evalScript;
    private final String tokenDir;

    public ModelTester(Configuration config) {
        charOffset = config.getBoolean("edu.cmu.cs.lti.output.character.offset", true);
//        evalLogOutputDir = FileUtils.joinPaths(config.get("edu.cmu.cs.lti.eval.log_dir"),
//                config.get("edu.cmu.cs.lti.experiment.name"));
        evalScript = config.get("edu.cmu.cs.lti.eval.script");
        tokenDir = config.get("edu.cmu.cs.lti.training.token_map.dir");

        String processOutputDir = FileUtils.joinPaths(config.get("edu.cmu.cs.lti.process.base.dir"),
                config.get("edu.cmu.cs.lti.experiment.name"));

        this.middleResults = processOutputDir + "/intermediate";
    }

    /**
     * @param modelConfig
     * @param reader
     * @param typeSystemDescription
     * @param sliceSuffix
     * @param runName
     * @param outputDir
     * @param resultDir
     * @param gold
     * @return System output of the collection.
     * @throws SAXException
     * @throws UIMAException
     * @throws CpeDescriptorException
     * @throws IOException
     * @throws InterruptedException
     */
    CollectionReaderDescription run(Configuration modelConfig, CollectionReaderDescription reader,
                                    TypeSystemDescription typeSystemDescription, String sliceSuffix, String runName,
                                    String outputDir, String resultDir, File gold)
            throws SAXException, UIMAException, CpeDescriptorException, IOException, InterruptedException {
        logger.info(String.format("Current run name is %s", runName));

        String baseDir = FileUtils.joinPaths(middleResults, sliceSuffix, runName);

        CollectionReaderDescription output = runModel(modelConfig, reader, outputDir, baseDir);

        String tbfOutput = FileUtils.joinPaths(resultDir, runName + ".tbf");
        RunnerUtils.writeResults(output, typeSystemDescription, tbfOutput, runName, charOffset, false);

        if (gold != null && gold.isFile()) {
            logger.info("Evaluating over all event types, gold is from: " + gold);
            eval(gold, tbfOutput, runName, sliceSuffix, null);

            if (modelConfig != null) {
                String selectedTypePath = modelConfig.get("edu.cmu.cs.lti.eval.selected_type.file");
                if (selectedTypePath != null) {
                    logger.info("Evaluating on selected event types.");
                    eval(gold, tbfOutput, runName, sliceSuffix, selectedTypePath);
                }
            }
        }
        return output;
    }

    private void eval(File gold, String system, String runName, String suffix, String typesPath)
            throws IOException, InterruptedException {
        boolean useSelectedType = typesPath != null;

        String evalDir;

        if (useSelectedType) {
            String typeName = FilenameUtils.removeExtension(FilenameUtils.getBaseName(typesPath));
            evalDir = FileUtils.joinPaths(new File(system).getParent(), runName + "_" + typeName);
        } else {
            evalDir = FileUtils.joinPaths(new File(system).getParent(), runName);
        }

        String evalLog = FileUtils.joinPaths(evalDir, "scoring_log.txt");
        FileUtils.ensureDirectory(evalDir);

        logger.info("Evaluating with " + evalScript + ", saving results to " + evalDir);
        logger.info("Gold file is " + gold);
        logger.info("System file is " + system);
        logger.info("Log file is " + evalLog);

        // TODO: having python2 here make it less portable.
        List<String> commands = new ArrayList<>(Arrays.asList(
                "python2", evalScript, "-g", gold.getPath(), "-s", system,
                "-d", FileUtils.joinPaths(evalDir, suffix + ".cmp"),
                "-o", FileUtils.joinPaths(evalDir, suffix + ".scores"),
                "-c", FileUtils.joinPaths(evalDir, suffix + ".coref_out"),
                "-a", FileUtils.joinPaths(evalDir, suffix + "_sequencing")
        ));

        if (!charOffset) {
            commands.add("-t");
            commands.add(tokenDir);
        }

        if (useSelectedType) {
            commands.add("-wl");
            commands.add(typesPath);
        }

        ProcessBuilder pb = new ProcessBuilder(commands.toArray(new String[commands.size()]));
        pb.redirectErrorStream();
        pb.redirectOutput(new File(evalLog));

        Process p = pb.start();

        Thread thread = new Thread(() -> {
            try {
                p.waitFor();
                int exitValue = p.exitValue();
                if (exitValue == 0) {
                    logger.info("Evaluation done successfully.");
                } else {
                    logger.info(String.format("Evaluation exit with error code %d, please check the evaluation log."
                            , exitValue));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    protected abstract CollectionReaderDescription runModel(Configuration taskConfig,
                                                            CollectionReaderDescription reader,
                                                            String mainDir, String baseDir)
            throws SAXException, UIMAException, CpeDescriptorException, IOException;
}