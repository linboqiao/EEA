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
public abstract class AbstractMentionPairFeatures {
    protected final Logger logger;

    protected Configuration featureConfig;
    protected Configuration generalConfig;

    public AbstractMentionPairFeatures(Configuration generalConfig, Configuration featureConfig) {
        this.logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Register feature extractor : " + featureName());
        this.featureConfig = featureConfig;
        this.generalConfig = generalConfig;
    }

    public abstract void initDocumentWorkspace(JCas context);

    /**
     * Extract features from the mention pair.
     *
     * @param documentContext The UIMA context
     * @param rawFeatures     Features will be added to this raw feature map.
     * @param firstAnno       First mention to extract from.
     * @param secondAnno      Second mention to extract from.
     */
    public abstract void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures,
                                 EventMention firstAnno, EventMention secondAnno);

    /**
     * Extract features from one mention only when the other is deliberately omitted, for example, the other mention
     * is a virtual root.
     *
     * @param documentContext The UIMA context
     * @param rawFeatures     Features will be added to this raw feature map.
     * @param secondAnno      Second mention to extract from.
     */
    public abstract void extract(JCas documentContext, TObjectDoubleMap<String> rawFeatures, EventMention secondAnno);

    /**
     * A name to describe this feature, it use the full class name by default
     *
     * @return Name of a feature function.
     */
    public String featureName() {
        return this.getClass().getName();
    }

    protected void addBoolean(TObjectDoubleMap<String> rawFeatures, String featureName) {
        rawFeatures.put(featureName, 1);
    }

}
