package edu.cmu.cs.lti.event_coref.annotators.misc;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.cmu.cs.lti.utils.MentionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsrJsonFormatter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File jsonOutput;

    public static final String PARAM_SYSTEM_ID = "systemId";
    @ConfigurationParameter(name = PARAM_SYSTEM_ID)
    private String systemId;

    private String id_prefix = "cmu_event_hector:";

    Writer writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        FileUtils.ensureDirectory(jsonOutput.getParent());

        try {
            writer = new BufferedWriter(new FileWriter(jsonOutput));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    private Pair<String, String> getSpanInfo(ComponentAnnotation mention) {
        return Pair.with(mention.getBegin() + "," + mention.getEnd(),
                mention.getCoveredText().replace("\n", " ").replace("\t", " "));
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        int eventMentionIndex = 1;

        JCas initialView = JCasUtil.getView(aJCas, CAS.NAME_DEFAULT_SOFA, aJCas);
        Article article = JCasUtil.selectSingle(initialView, Article.class);
        String articleName = article.getArticleName();

        SourceDocumentInformation sourceInfo = JCasUtil.selectSingle(initialView, SourceDocumentInformation.class);

        DocInfo docInfo = new DocInfo(id_prefix + articleName, sourceInfo.getUri(), articleName);

        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            Pair<String, String> wordInfo = getSpanInfo(mention);
            if (wordInfo == null) {
                continue;
            }
            String eid = "E" + eventMentionIndex++;

            int[] mentionSpan = {mention.getBegin(), mention.getEnd()};
            String realis = mention.getRealisType() == null ? "Actual" : mention.getRealisType();
            DocInfo.MentionInterpretation interp = new DocInfo.MentionInterpretation("text_mention", systemId, eid, mentionSpan,
                    wordInfo.getValue1(), mention.getEventType(), realis);

            FSList argLinksFS = mention.getArguments();

            if (argLinksFS != null) {
                for (EventMentionArgumentLink link : FSCollectionFactory.create(argLinksFS, EventMentionArgumentLink.class)) {
                    EntityMention arg = link.getArgument();
                    int[] argSpan = {arg.getBegin(), arg.getEnd()};
                    String argText = arg.getCoveredText().replace("\n", " ");
                    interp.addArgument(link.getFrameElementName(), link.getPropbankRoleName(), link.getVerbNetRoleName(), argSpan, argText);
                }
            }

            docInfo.addInterp(interp);
        }

        int relationIndex = 1;
//        logger.info("Number of clusters : " + JCasUtil.select(aJCas//, Event.class).size());
        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            if (event.getEventMentions().size() > 1) {
                DocInfo.EventInterpretation interp = new DocInfo.EventInterpretation("mention_relation", systemId,
                    "R" + relationIndex, "Coreference");
                for (EventMention mention : FSCollectionFactory.create(event.getEventMentions(), EventMention.class)) {
                    String mentionId = mention.getId();
                    interp.addMember(mentionId);
                }
                relationIndex ++;
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            writer.write(gson.toJson(docInfo));
            writer.write("\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    static class DocInfo {
        private String id;
        private String sourceID;
        private String raw_input;
        private List<Interpretation> interp;

        public DocInfo(String id, String sourceID, String raw_input) {
            this.id = id;
            this.sourceID = sourceID;
            this.raw_input = raw_input;
            this.interp = new ArrayList<>();
        }

        public void addInterp(Interpretation interp) {
            this.interp.add(interp);
        }

        static class Interpretation {
            private String record_type;
            private String model;
            private String id;

            public Interpretation(String record_type, String model, String id) {
                this.record_type = record_type;
                this.model = model;
                this.id = id;
            }
        }

        static class EventInterpretation extends Interpretation {
            private String relation_type;
            private List<String> members;

            public EventInterpretation(String record_type, String model, String id, String relation_type) {
                super(record_type, model, id);
                this.relation_type = relation_type;
                this.members = new ArrayList<>();
            }

            public void addMember(String memberId){
                members.add(memberId);
            }
        }

        static class MentionInterpretation extends Interpretation {
            private int[] char_span;
            private String surface;
            private String mention_type;
            private String realis;
            private List<Argument> arguments;

            public MentionInterpretation(String record_type, String model, String id, int[] char_span, String surface,
                                         String mention_type, String realis) {
                super(record_type, model, id);
                this.char_span = char_span;
                this.surface = surface;
                this.mention_type = mention_type;
                this.realis = realis;
                this.arguments = new ArrayList<>();
            }

            public void addArgument(String frameNetRole, String propbankRole, String verbNetRole, int[] char_span,
                                    String entity) {
                this.arguments.add(new Argument(frameNetRole, propbankRole, verbNetRole, char_span, entity));
            }

            static class Argument {
                private String frameNetRole;
                private String PropbankRole;
                private String VerbNetRole;
                private int[] char_span;
                private String entity;

                Argument(String frameNetRole, String propbankRole, String verbNetRole, int[] char_span,
                         String entity) {
                    this.frameNetRole = frameNetRole != null ? frameNetRole : "-";
                    PropbankRole = propbankRole != null ? propbankRole : "-";
                    VerbNetRole = verbNetRole != null ? verbNetRole : "-";
                    this.char_span = char_span;
                    this.entity = entity;
                }
            }
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        String typeSystemName = "TaskEventMentionDetectionTypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        String inputDir = argv[0];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);


        AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(CsrJsonFormatter.class,
                CsrJsonFormatter.PARAM_SYSTEM_ID, "VanillaMention",
                CsrJsonFormatter.PARAM_OUTPUT_FILE, argv[1]
        );

        SimplePipeline.runPipeline(reader, engine);
    }
}
