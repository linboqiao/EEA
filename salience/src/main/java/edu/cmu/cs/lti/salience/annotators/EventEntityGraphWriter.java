package edu.cmu.cs.lti.salience.annotators;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.salience.utils.TextUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/2/18
 * Time: 2:03 PM
 *
 * @author Zhengzhong Liu
 */
public class EventEntityGraphWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private File outputDir;

    private BufferedWriter dependencyWriter;
    private BufferedWriter semanticWriter;
    private BufferedWriter sameSentenceWriter;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        logger.info("Output directory is: " + outputDir);

        try {
            dependencyWriter = getWriter(new File(outputDir, "dependency.gz"));
            semanticWriter = getWriter(new File(outputDir, "semantic.gz"));
            sameSentenceWriter = getWriter(new File(outputDir, "sameSentence.gz"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        //Make sure index is correct.
        int index = 0;
        for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, body)) {
            eventMention.setIndex(index);
            index++;
        }

        ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> dependencyAdjacent = ArrayListMultimap
                .create();
        ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> sameSentenceAdjacent = ArrayListMultimap
                .create();
        ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> semanticAdjacent = ArrayListMultimap
                .create();

        for (StanfordCorenlpSentence sentence : JCasUtil.selectCovered(StanfordCorenlpSentence.class, body)) {
            List<EventMention> events = JCasUtil.selectCovered(EventMention.class, sentence);
            List<GroundedEntity> entities = JCasUtil.selectCovered(GroundedEntity.class, sentence);
            Map<Word, GroundedEntity> head2Entities = headwordMapping(entities);

            getDependencyRelations(events, head2Entities, dependencyAdjacent);
            getSameSentenceRelations(events, head2Entities, sameSentenceAdjacent);
            getSemanticRelations(events, head2Entities, semanticAdjacent);
        }

        try {
            writeAdjacent(aJCas, body, dependencyWriter, dependencyAdjacent);
            writeAdjacent(aJCas, body, semanticWriter, semanticAdjacent);
            writeAdjacent(aJCas, body, sameSentenceWriter, sameSentenceAdjacent);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            dependencyWriter.close();
            semanticWriter.close();
            sameSentenceWriter.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private BufferedWriter getWriter(File file) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))));
    }

    private List<Integer> getLoc(ArticleComponent articleComponent, ComponentAnnotation anno) {
        Span tokenOffset = TextUtils.getSpaceTokenOffset(articleComponent, anno);
        return Arrays.asList(tokenOffset.getBegin(), tokenOffset.getEnd());
    }

    private void writeAdjacent(JCas jCas, ArticleComponent component, Writer writer, ArrayListMultimap<EventMention,
            Pair<String, GroundedEntity>> adjacent) throws IOException {
        Gson gson = new Gson();
        String docid = UimaConvenience.getArticleName(jCas);

        AdjacentStructure as = new AdjacentStructure();
        as.docno = docid;

        List<EventAdjacent> adjacentList = new ArrayList<>();

        for (Map.Entry<EventMention, Collection<Pair<String, GroundedEntity>>> eventArgs : adjacent
                .asMap().entrySet()) {
            EventMention evm = eventArgs.getKey();
            EventAdjacent ea = new EventAdjacent();
            ea.id = Integer.toString(evm.getIndex());
            ea.surface = TextUtils.asTokenized(evm);
            ea.loc = getLoc(component, evm);

            List<LinkedEntity> argEntities = new ArrayList<>();

            for (Pair<String, GroundedEntity> entityRel : eventArgs.getValue()) {
                String rel = entityRel.getKey();
                GroundedEntity entity = entityRel.getValue();

                LinkedEntity le = new LinkedEntity();
                le.id = entity.getKnowledgeBaseId();
                le.link = rel;
                le.loc = getLoc(component, entity);
                argEntities.add(le);
            }
            ea.entities = argEntities;

            adjacentList.add(ea);
        }
        as.adjacentList = adjacentList;

        String jsonStr = gson.toJson(as);

        writer.write(jsonStr + "\n");
    }

    class AdjacentStructure {
        public String docno;
        public List<EventAdjacent> adjacentList;
    }

    class EventAdjacent {
        public String surface;
        public List<Integer> loc;
        public String id;
        public List<LinkedEntity> entities;
    }

    class LinkedEntity {
        public String link;
        public String id;
        public List<Integer> loc;
    }

    private <T extends ComponentAnnotation> Map<Word, T> headwordMapping(Collection<T> annotations) {
        Map<Word, T> mapping = new HashMap<>();
        for (T annotation : annotations) {
            Word headword = UimaNlpUtils.findHeadFromStanfordAnnotation(annotation);
            if (headword != null) {
                mapping.put(headword, annotation);
            }
        }
        return mapping;
    }

    private void getDependencyRelations(List<EventMention> events, Map<Word, GroundedEntity> head2Entities,
                                        ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> adjacent) {
        for (EventMention event : events) {
            Word headword = event.getHeadWord();
            FSList childDepFS = headword.getChildDependencyRelations();

            if (childDepFS != null) {
                Collection<StanfordDependencyRelation> childDeps = FSCollectionFactory.create(childDepFS,
                        StanfordDependencyRelation.class);

                for (StanfordDependencyRelation dep : childDeps) {
                    Word child = dep.getChild();
                    GroundedEntity childEntity = head2Entities.get(child);
                    if (childEntity != null) {
                        adjacent.put(event, Pair.of(dep.getDependencyType(), childEntity));
                    }
                }
            }
        }
    }

    private void getSameSentenceRelations(List<EventMention> events, Map<Word, GroundedEntity> head2Entities,
                                          ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> adjacent) {
        for (EventMention event : events) {
            for (GroundedEntity groundedEntity : head2Entities.values()) {
                adjacent.put(event, Pair.of("SameSentence", groundedEntity));
            }
        }
    }

    private void getSemanticRelations(List<EventMention> events, Map<Word, GroundedEntity> head2Entities,
                                      ArrayListMultimap<EventMention, Pair<String, GroundedEntity>> adjacent) {
        for (EventMention event : events) {
            FSList childDepFS = event.getArguments();
            if (childDepFS != null) {
                Collection<EventMentionArgumentLink> childDeps = FSCollectionFactory.create(childDepFS,
                        EventMentionArgumentLink.class);
                for (EventMentionArgumentLink dep : childDeps) {
                    Word child = dep.getArgument().getHead();
                    GroundedEntity childEntity = head2Entities.get(child);
                    if (childEntity != null) {
                        adjacent.put(event, Pair.of(dep.getFrameElementName(), childEntity));
                    }
                }
            }
        }
    }
}
