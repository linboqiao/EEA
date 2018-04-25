package edu.cmu.cs.lti.io;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.DispatchReader;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Rich content from events requires slightly richer writer.
 * Date: 4/25/18
 * Time: 9:25 AM
 *
 * @author Zhengzhong Liu
 */
public class JsonRichEventWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_FILE = "outputFile";

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    int objectIndex;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Gson gson = new Gson();

        objectIndex = 0;
        try {
            FileUtils.write(outputFile, gson.toJson(buildJson(aJCas)) + "\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    public static void main(String[] args) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputPath = args[0];
        String inputType = args[1];
        String outputPath = args[2];

        CollectionReaderDescription reader = DispatchReader.getReader(typeSystemDescription, inputPath, inputType,
                null);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                JsonRichEventWriter.class, typeSystemDescription,
                JsonRichEventWriter.PARAM_OUTPUT_FILE, outputPath
        );

        SimplePipeline.runPipeline(reader, writer);
    }

    private JsonEntityMention createEntity(ComponentAnnotation anno) {
        JsonEntityMention jsonEnt = new JsonEntityMention(objectIndex++, anno);

        StanfordCorenlpToken uimaHead = UimaNlpUtils.findHeadFromStanfordAnnotation(anno);
        JsonWord jsonHead = new JsonWord(objectIndex++, uimaHead);
        jsonHead.lemma = uimaHead.getLemma();
        jsonEnt.headWord = jsonHead;
        return jsonEnt;
    }


    private Document buildJson(JCas aJCas) {
        String docid = UimaConvenience.getArticleName(aJCas);

        Document doc = new Document();
        doc.docid = docid;

        doc.eventMentions = new ArrayList<>();

        Map<EventMention, JsonEventMention> evmMap = new HashMap<>();

        Map<StanfordCorenlpToken, JsonEntityMention> jsonEntMap = new HashMap<>();

        Map<StanfordCorenlpToken, EntityMention> entMap = new HashMap<>();

        for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
            StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
            entMap.put(head, mention);
            jsonEntMap.put(head, createEntity(mention));
        }

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            JsonEventMention jsonEvm = new JsonEventMention(objectIndex++, mention);
            Word headword = mention.getHeadWord();

            jsonEvm.headWord = new JsonWord(objectIndex++, headword);

            jsonEvm.arguments = new ArrayList<>();
            for (Map.Entry<SemanticArgument, Collection<String>> argument : getArguments(headword).asMap()
                    .entrySet()) {
                SemanticArgument arg = argument.getKey();

                StanfordCorenlpToken argHead = UimaNlpUtils.findHeadFromStanfordAnnotation(arg);

                JsonEntityMention jsonEnt;
                if (entMap.containsKey(argHead)) {
                    jsonEnt = jsonEntMap.get(argHead);
                } else {
                    jsonEnt = createEntity(arg);
                    jsonEntMap.put(argHead, jsonEnt);
                }

                for (String role : argument.getValue()) {
                    JsonArgument jsonArg = new JsonArgument();
                    jsonArg.roles = new ArrayList<>();
                    jsonArg.entity = jsonEnt;
                    jsonArg.event = jsonEvm;
                    jsonArg.roles.add(role);
                }
            }

            doc.eventMentions.add(jsonEvm);
            evmMap.put(mention, jsonEvm);
        }

        doc.relations = new ArrayList<>();
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            if (event.getEventMentions().size() > 1) {
                JsonRelation rel = new JsonRelation();
                rel.relationType = "event_coreference";
                rel.arguments = new ArrayList<>();

                for (EventMention eventMention : FSCollectionFactory.create(event.getEventMentions(), EventMention
                        .class)) {
                    JsonEventMention jsonEvm = evmMap.get(eventMention);
                    rel.arguments.add(jsonEvm.id);
                }
                doc.relations.add(rel);
            }
        }

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            if (entity.getEntityMentions().size() > 1) {
                JsonRelation rel = new JsonRelation();
                rel.relationType = "entity_coreference";
                rel.arguments = new ArrayList<>();

                for (EntityMention ent : FSCollectionFactory.create(entity.getEntityMentions(), EntityMention
                        .class)) {
                    StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(ent);
                    if (jsonEntMap.containsKey(head)) {
                        JsonEntityMention jsonEnt = jsonEntMap.get(head);
                        rel.arguments.add(jsonEnt.id);
                    } else {
                        System.out.println("Entity mention not found " + ent.getCoveredText() + " " + ent
                                .getBegin());
                    }
                }
                doc.relations.add(rel);
            }
        }

        doc.entityMentions = new ArrayList<>();
        doc.entityMentions.addAll(jsonEntMap.values());

        return doc;
    }


    private ArrayListMultimap<SemanticArgument, String> getArguments(Word headWord) {
        FSList semanticRelationsFS = headWord.getChildSemanticRelations();
        ArrayListMultimap<SemanticArgument, String> args = ArrayListMultimap.create();

        if (semanticRelationsFS != null) {
            for (SemanticRelation semanticRelation : FSCollectionFactory.create(semanticRelationsFS, SemanticRelation
                    .class)) {
                SemanticArgument argument = semanticRelation.getChild();

                String pbName = semanticRelation.getPropbankRoleName();
                String fnName = semanticRelation.getFrameElementName();

                if (pbName != null) {
                    args.put(argument, "pb:" + pbName);
                }

                if (fnName != null) {
                    args.put(argument, "fn:" + fnName);
                }
            }
        }
        return args;
    }


    class Document {
        String docid;
        List<JsonEventMention> eventMentions;
        List<JsonEntityMention> entityMentions;
        List<JsonRelation> relations;
    }

    class DiscourseObject {
        int id;
        List<Integer> span;

        public DiscourseObject(int id, ComponentAnnotation anno) {
            this.id = id;
            setSpan(anno);
        }

        void setSpan(ComponentAnnotation anno) {
            span = Arrays.asList(anno.getBegin(), anno.getEnd());
        }
    }

    class JsonEventMention extends DiscourseObject {
        JsonWord headWord;
        List<JsonArgument> arguments;

        public JsonEventMention(int id, ComponentAnnotation anno) {
            super(id, anno);
        }
    }

    class JsonEntityMention extends DiscourseObject {
        JsonWord headWord;

        public JsonEntityMention(int id, ComponentAnnotation anno) {
            super(id, anno);
        }
    }

    class JsonWord extends DiscourseObject {
        String lemma;

        public JsonWord(int id, ComponentAnnotation anno) {
            super(id, anno);
        }
    }

    class JsonRelation {
        List<Integer> arguments;
        String relationType;
    }

    class JsonArgument {
        JsonEventMention event;
        JsonEntityMention entity;
        List<String> roles;
    }

}
