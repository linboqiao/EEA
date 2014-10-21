package edu.cmu.cs.lti.cds.annotators.script.karlmooney;

import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import edu.cmu.cs.lti.ling.PropBankTagSet;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.CollectionUtils;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.HTreeMap;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/20/14
 * Time: 3:47 PM
 */
public class KarlMooneyScriptCounter extends AbstractLoggingAnnotator {

    public static final String PARAM_DB_DIR_PATH = "dbLocation";

    public static final String PARAM_SKIP_BIGRAM_N = "skippedBigramN";


    private HTreeMap<Fun.Tuple2<MooneyEventRepre, MooneyEventRepre>, Integer> cooccCounts;

    private int skippedBigramN;

    private DB db;

    private LinkedHashMap<String, Integer> targetArguments;

    public static final String defaultDBName = "coocc";

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String dbPath = (String) aContext.getConfigParameterValue(PARAM_DB_DIR_PATH);
        skippedBigramN = (Integer) aContext.getConfigParameterValue(PARAM_SKIP_BIGRAM_N);

        targetArguments = new LinkedHashMap<>();
        targetArguments.put(PropBankTagSet.ARG0, MooneyEventRepre.firstArg0Marker);
        targetArguments.put(PropBankTagSet.ARG1, MooneyEventRepre.firstArg1Marker);
        targetArguments.put(PropBankTagSet.ARG2, MooneyEventRepre.firstArg2Marker);

        db = DBMaker.newFileDB(new File(dbPath, defaultDBName)).closeOnJvmShutdown().make();
        cooccCounts = db.getHashMap("subsituted_event_cooccurrences");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        Collection<EventMention> allMentions = JCasUtil.select(aJCas, EventMention.class);
        List<Pair<EventMention, EventMention>> mentionBigrams = CollectionUtils.nSkippedBigrams(allMentions, skippedBigramN);

        for (Pair<EventMention, EventMention> bigram : mentionBigrams) {
            Fun.Tuple2<MooneyEventRepre, MooneyEventRepre> subsitutedBigram = firstBasedSubstitution(aJCas, bigram.getLeft(), bigram.getRight());

            Integer oldCount = cooccCounts.get(subsitutedBigram);

            if (oldCount == null) {
                cooccCounts.put(subsitutedBigram, 1);
            } else {
                cooccCounts.put(subsitutedBigram, oldCount + 1);
            }
        }

    }

    private Fun.Tuple2<MooneyEventRepre, MooneyEventRepre> firstBasedSubstitution(JCas aJCas, EventMention evm1, EventMention evm2) {
        TIntIntHashMap evm1Args = new TIntIntHashMap();

        TIntIntHashMap evm1Slots = new TIntIntHashMap();

        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm1.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();
            if (targetArguments.containsKey(argumentRole)) {
                int slotId = targetArguments.get(argumentRole);
                evm1Args.put(entityIdToInteger(aLink.getArgument().getReferingEntity().getId()), slotId);
                //substitution for the first event is always self
                evm1Slots.put(slotId, slotId);
            }
        }

        MooneyEventRepre evmRepre1 = new MooneyEventRepre(getFanseLemma(aJCas, evm1.getHeadWord()),
                evm1Slots.containsKey(MooneyEventRepre.firstArg0Marker) ? evm1Slots.get(MooneyEventRepre.firstArg0Marker) : MooneyEventRepre.nullArgMarker,
                evm1Slots.containsKey(MooneyEventRepre.firstArg1Marker) ? evm1Slots.get(MooneyEventRepre.firstArg1Marker) : MooneyEventRepre.nullArgMarker,
                evm1Slots.containsKey(MooneyEventRepre.firstArg2Marker) ? evm1Slots.get(MooneyEventRepre.firstArg2Marker) : MooneyEventRepre.nullArgMarker
        );

        TIntIntHashMap evm2Slots = new TIntIntHashMap();
        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(evm2.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();

            if (targetArguments.containsKey(argumentRole)) {
                int entityId = entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                //substitution for the second event is based on the first event mention
                int substituteId = evm1Args.containsKey(entityId) ? evm1Args.get(entityId) : MooneyEventRepre.otherMarker;
                int slotId = targetArguments.get(argumentRole);
                evm2Slots.put(slotId, substituteId);
            }
        }

        MooneyEventRepre evmRepre2 = new MooneyEventRepre(getFanseLemma(aJCas, evm2.getHeadWord()),
                evm2Slots.containsKey(MooneyEventRepre.firstArg0Marker) ? evm2Slots.get(MooneyEventRepre.firstArg0Marker) : MooneyEventRepre.nullArgMarker,
                evm2Slots.containsKey(MooneyEventRepre.firstArg1Marker) ? evm2Slots.get(MooneyEventRepre.firstArg1Marker) : MooneyEventRepre.nullArgMarker,
                evm2Slots.containsKey(MooneyEventRepre.firstArg2Marker) ? evm2Slots.get(MooneyEventRepre.firstArg2Marker) : MooneyEventRepre.nullArgMarker
        );

        return new Fun.Tuple2<>(evmRepre1, evmRepre2);
    }

    private String getFanseLemma(JCas aJCas, Word token) {
        StanfordCorenlpToken sToken = UimaConvenience.selectCoveredFirst(aJCas, token, StanfordCorenlpToken.class);
        if (sToken != null) {
            return sToken.getLemma();
        } else {
            return token.getCoveredText();
        }
    }

    private int entityIdToInteger(String eid) {
        return Integer.parseInt(eid);
    }


}
