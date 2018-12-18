package edu.cmu.cs.lti.learning.runners;

import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/11/16
 * Time: 9:55 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractMentionModelRunner {
    protected final TypeSystemDescription typeSystemDescription;
    protected final String eventModelDir;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
//    protected final String trainingWorkingDir;
//    protected final String testingWorkingDir;
    protected final String processOut;

    protected final Configuration mainConfig;

    protected final static String fullRunSuffix = "all";
    protected final String language;


    public AbstractMentionModelRunner(Configuration mainConfig, TypeSystemDescription typeSystemDescription) {
        this.typeSystemDescription = typeSystemDescription;
//        this.trainingWorkingDir = mainConfig.get("edu.cmu.cs.lti.training.working.dir");
//        this.testingWorkingDir = mainConfig.get("edu.cmu.cs.lti.test.working.dir");
        this.processOut = FileUtils.joinPaths(mainConfig.get("edu.cmu.cs.lti.process.base.dir"),
                mainConfig.get("edu.cmu.cs.lti.experiment.name"));
        this.eventModelDir = mainConfig.get("edu.cmu.cs.lti.model.event.dir");
        this.mainConfig = mainConfig;
        this.language = mainConfig.getOrElse("edu.cmu.cs.lti.language", "en");

    }
}
