package edu.cmu.cs.lti.script.annotators.stats;

import edu.cmu.cs.lti.script.runners.stats.ArgumentStatisticsCounterRunner;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/15/14
 * Time: 5:39 PM
 */
public class ArgumentStatisticsCounter extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
            ArgumentStatisticsCounterRunner.numEvents++;
            Map<String,String> roles = new HashMap<>();

            for (EventMentionArgumentLink link : FSCollectionFactory.create(evm.getArguments(), EventMentionArgumentLink.class)) {
                String roleType = link.getArgumentRole();
                String componentId = link.getComponentId();

                //in this way, each role type only being counted once
                roles.put(roleType,componentId);
            }

            for (Map.Entry<String,String> roleEntry : roles.entrySet()){
                int count = 1;
                String roleType = roleEntry.getKey();
                String componentId = roleEntry.getValue();
                if (ArgumentStatisticsCounterRunner.argumentCounts.contains(roleType, componentId)) {
                    count += ArgumentStatisticsCounterRunner.argumentCounts.get(roleType, componentId);
                }
                ArgumentStatisticsCounterRunner.argumentCounts.put(roleType, componentId, count);
            }
        }
    }
}