package edu.cmu.cs.lti.emd.stat;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.io.writer.AbstractPlainTextAggregator;
import edu.emory.mathcs.backport.java.util.Collections;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/20/16
 * Time: 3:15 PM
 *
 * @author Zhengzhong Liu
 */
public class EventMentionSurfaceCollector extends AbstractPlainTextAggregator {

    private TObjectIntMap<String> lemmaCounts = new TObjectIntHashMap<>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            lemmaCounts.adjustOrPutValue(getSurface(eventMention), 1, 1);
        }
    }

    private String getSurface(EventMention mention){
        return mention.getCoveredText().replaceAll("\\s", "").replaceAll("\\p{Punct}", "");
    }

    @Override
    public String getAggregatedTextToPrint() {
        StringBuilder sb = new StringBuilder();

        List<Pair<String, Integer>> counts = new ArrayList<>();

        for (TObjectIntIterator<String> iter = lemmaCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            counts.add(Pair.of(iter.key(), iter.value()));
        }

        Collections.sort(counts, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Pair<String, Integer> c1 = (Pair<String, Integer>) o1;
                Pair<String, Integer> c2 = (Pair<String, Integer>) o2;

                // Note we use descending order.
                return new CompareToBuilder().append(c2.getValue(), c1.getValue()).append(c1.getKey(), c2.getKey())
                        .toComparison();
            }
        });

        for (Pair<String, Integer> count : counts) {
            sb.append(String.format("%s\t%d\n", count.getKey(), count.getValue()));
        }

        return sb.toString();
    }

}
