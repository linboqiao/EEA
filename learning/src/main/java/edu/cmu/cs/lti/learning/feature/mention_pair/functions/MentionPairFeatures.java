package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/27/15
 * Time: 5:18 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class MentionPairFeatures {
    protected final Logger logger;

    protected Configuration featureConfig;
    protected Configuration generalConfig;

    public MentionPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        this.logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Register feature extractor : " + featureName());
        this.featureConfig = featureConfig;
        this.generalConfig = generalConfig;
    }

    public abstract void initDocumentWorkspace(JCas context);

    public abstract void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                                 EventMention firstAnno, EventMention secondAnno);

    /**
     * A name to describe this feature, it use the full class name by default
     *
     * @return Name of a feature function.
     */
    public String featureName() {
        return this.getClass().getName();
    }

}
