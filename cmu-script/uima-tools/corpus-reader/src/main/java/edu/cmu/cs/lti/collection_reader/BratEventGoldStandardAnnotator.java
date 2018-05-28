package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.model.BratConstants;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.Comparators;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/11/15
 * Time: 3:25 PM
 *
 * @author Zhengzhong Liu
 */
public class BratEventGoldStandardAnnotator extends AbstractAnnotator {
    public static final String PARAM_ANNOTATION_DIR = "annotationDir";
    public static final String PARAM_TOKENIZATION_MAP_DIR = "tokenDir";

    public static final String PARAM_ANNOTATION_FILE_NAME_SUFFIX = "annotationFileNameSuffix";
    public static final String PARAM_TOKEN_OFFSET_SUFFIX = "tokenOffsetSuffix";
    public static final String PARAM_TEXT_FILE_SUFFIX = "textFileSuffix";
    public static final String PARAM_TOKEN_OFFSET_BEGIN_FIELD_NUM = "tokenOffsetBeginFieldNum";
    public static final String PARAM_TOKEN_OFFSET_END_FIELD_NUM = "tokenOffsetEndFieldNum";

    @ConfigurationParameter(name = PARAM_ANNOTATION_DIR)
    private File annotationDir;

    @ConfigurationParameter(name = PARAM_TOKENIZATION_MAP_DIR)
    private File tokenizationDir;

    @ConfigurationParameter(name = PARAM_ANNOTATION_FILE_NAME_SUFFIX, mandatory = false)
    private String annotationFileNameSuffix;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_SUFFIX, mandatory = false)
    private String tokenOffsetSuffix;

    @ConfigurationParameter(name = PARAM_TEXT_FILE_SUFFIX, mandatory = false)
    private String textFileNameSuffix;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_BEGIN_FIELD_NUM, mandatory = false)
    private Integer tokenOffsetBeginFieldNumber;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_END_FIELD_NUM, mandatory = false)
    private Integer tokenOffsetEndFieldNumber;

    public static final String defaultAnnotationFileNameSuffix = ".tkn.ann";
    public static final String defaultTokenizationFileNameSuffix = ".txt.tab";
    public static final String defaultTextFileNameSuffix = ".tkn.txt";
    public static final int defaultTokenBeginFieldNumber = 2;
    public static final int defaultTokenEndFieldNumber = 3;

    private static final String realisTypeName = "Realis";
    private static final String coreferenceLinkName = "Coreference";
    private static final String afterLinkName = "After";
    private static final String subeventLinkName = "Subevent";

    private static final Logger logger = LoggerFactory.getLogger(BratEventGoldStandardAnnotator.class);

    public static final String COMPONENT_ID = BratEventGoldStandardAnnotator.class.getSimpleName();

    private Map<String, File> annotationsByName;
    private Map<String, File> offsetsByName;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        if (tokenOffsetSuffix == null) {
            tokenOffsetSuffix = defaultTokenizationFileNameSuffix;
        }

        if (annotationFileNameSuffix == null) {
            annotationFileNameSuffix = defaultAnnotationFileNameSuffix;
        }

        if (textFileNameSuffix == null) {
            textFileNameSuffix = defaultTextFileNameSuffix;
        }

        if (tokenOffsetBeginFieldNumber == null) {
            tokenOffsetBeginFieldNumber = defaultTokenBeginFieldNumber;
        }

        if (tokenOffsetEndFieldNumber == null) {
            tokenOffsetEndFieldNumber = defaultTokenEndFieldNumber;
        }

        if (!annotationDir.isDirectory()) {
            throw new IllegalArgumentException("Cannot find annotation directory " + annotationDir.getAbsolutePath());
        }

        File[] annotationDocuments = edu.cmu.cs.lti.utils.FileUtils.getFilesWithSuffix(annotationDir,
                annotationFileNameSuffix);
        File[] offsetDocuments = edu.cmu.cs.lti.utils.FileUtils.getFilesWithSuffix(tokenizationDir, tokenOffsetSuffix);

        annotationsByName = trimAsDocId(annotationDocuments, annotationFileNameSuffix);
        offsetsByName = trimAsDocId(offsetDocuments, tokenOffsetSuffix);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = null;
        try {
            goldView = aJCas.createView(goldStandardViewName);
        } catch (CASException e) {
            throw new RuntimeException(e);
        }
        goldView.setDocumentText(aJCas.getDocumentText());

        String plainDocId = StringUtils.removeEnd(UimaConvenience.getDocId(aJCas), textFileNameSuffix);

        File annotationDocument = annotationsByName.get(plainDocId);
        File tokenDocument = offsetsByName.get(plainDocId);

        try {
            annotateGoldStandard(goldView, FileUtils.readLines(annotationDocument, encoding), FileUtils.readLines
                    (tokenDocument, encoding));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, File> trimAsDocId(File[] annotationDocuments, String suffix) {
        Map<String, File> annotationDocByName = new HashMap<>();
        for (File annotationDocument : annotationDocuments) {
            String annotationDocName = StringUtils.removeEnd(annotationDocument.getName(), suffix);
            annotationDocByName.put(annotationDocName, annotationDocument);
        }
        return annotationDocByName;
    }

    private Map<String, EventMention> annotateMentionAttribute(JCas aJCas, List<String> eventIds, List<String>
            eventTextBounds, Map<String, Pair<List<Span>, String>> textBoundId2SpanAndType, ArrayListMultimap<String,
            Attribute> id2Attribute, ArrayListMultimap<String, Span> textBoundId2TokenSpan) {
        Map<String, EventMention> id2Mentions = new HashMap<>();
        for (int i = 0; i < eventIds.size(); i++) {
            String eventId = eventIds.get(i);
            String eventTextBoundId = eventTextBounds.get(i);
            Pair<List<Span>, String> eventInfo = textBoundId2SpanAndType.get(eventTextBoundId);
            EventMention eventMention = new EventMention(aJCas);
            eventMention.setEventType(eventInfo.getValue1());
            List<Span> tokenizedEventMentionSpans = textBoundId2TokenSpan.get(eventTextBoundId);

            eventMention.setRegions(new FSArray(aJCas, tokenizedEventMentionSpans.size()));

            int earliestBegin = Integer.MAX_VALUE;
            int latestEnd = 0;

            for (int spanIndex = 0; spanIndex < tokenizedEventMentionSpans.size(); spanIndex++) {
                Span span = tokenizedEventMentionSpans.get(spanIndex);
                if (span.getBegin() < earliestBegin) {
                    earliestBegin = span.getBegin();
                }
                if (span.getEnd() > latestEnd) {
                    latestEnd = span.getEnd();
                }
                Annotation region = new Annotation(aJCas, span.getBegin(), span.getEnd());
                eventMention.setRegions(spanIndex, region);
            }
            eventMention.setBegin(earliestBegin);
            eventMention.setEnd(latestEnd);


            id2Mentions.put(eventId, eventMention);
            for (Attribute attribute : id2Attribute.get(eventId)) {
                switch (attribute.attributeName) {
                    case realisTypeName:
                        eventMention.setRealisType(attribute.attributeValue);
                        break;
                    default:
                        logger.warn("Attribute Name not recognized : " + attribute.attributeName);
                }
            }
        }
        return id2Mentions;
    }

    private void annotateRelations(JCas aJCas, List<Relation> relations, Map<String, EventMention> id2Mentions) {
        List<Set<EventMention>> clusters = new ArrayList<>();

        for (Relation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            EventMention mention1 = id2Mentions.get(e1);
            EventMention mention2 = id2Mentions.get(e2);

            String relationName = relation.relationName;
            if (relationName.equals(coreferenceLinkName)) {
                boolean inCluster = false;
                for (Set<EventMention> cluster : clusters) {
                    if (cluster.contains(mention1)) {
                        cluster.add(mention2);
                        inCluster = true;
                        break;
                    } else if (cluster.contains(mention2)) {
                        cluster.add(mention1);
                        inCluster = true;
                        break;
                    }
                }
                if (!inCluster) {
                    Set<EventMention> newCluster = new HashSet<>();
                    newCluster.add(mention1);
                    newCluster.add(mention2);
                    clusters.add(newCluster);
                }
            } else if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
                //treat coref and sub links differently
                EventMentionRelation eventMentionRelation = new EventMentionRelation(aJCas);
                eventMentionRelation.setRelationType(relationName);
                eventMentionRelation.setHead(mention1);
                eventMentionRelation.setChild(mention2);
                mention1.setChildEventRelations(UimaConvenience.appendFSList(aJCas, mention1.getChildEventRelations()
                        , eventMentionRelation, EventMentionRelation.class));
                mention2.setHeadEventRelations(UimaConvenience.appendFSList(aJCas, mention2.getChildEventRelations(),
                        eventMentionRelation, EventMentionRelation.class));
                UimaAnnotationUtils.finishTop(eventMentionRelation, COMPONENT_ID, 0, aJCas);
            }
        }

        ArrayList<EventMention> discoursedSortedEventMentions = new ArrayList<>(id2Mentions.values());
        Collections.sort(discoursedSortedEventMentions, new Comparators.AnnotationSpanComparator<>());

        final int[] evmId = new int[1];
        discoursedSortedEventMentions.forEach(
                sortedEventMention -> UimaAnnotationUtils.finishAnnotation(sortedEventMention, COMPONENT_ID,
                        evmId[0]++, aJCas)
        );

        List<Event> allEvents = new ArrayList<>();
        Set<EventMention> mappedMentions = new HashSet<>();

        clusters.forEach(cluster -> {
            Event event = new Event(aJCas);
            List<EventMention> sortedCluster = cluster.stream().sorted(new Comparators.AnnotationSpanComparator<>())
                    .collect(Collectors.toList());
            event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, sortedCluster));
            cluster.forEach(mention -> {
                mention.setReferringEvent(event);
                mappedMentions.add(mention);
            });
            allEvents.add(event);
        });

        discoursedSortedEventMentions.stream().filter(
                mention -> !mappedMentions.contains(mention)
        ).forEach(
                sortedEventMention -> {
                    Event event = new Event(aJCas);
                    event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, Arrays.asList(sortedEventMention)));
                    sortedEventMention.setReferringEvent(event);
                    allEvents.add(event);
                }
        );

        int[] eventId = new int[1];
        int numNonSingleton = allEvents.stream().sorted((e1, e2) -> new Comparators.AnnotationSpanComparator<>()
                .compare(e1.getEventMentions(0), e2.getEventMentions(0))).mapToInt(
                event -> {
                    UimaAnnotationUtils.finishTop(event, COMPONENT_ID, eventId[0], aJCas);
                    event.setIndex(eventId[0]);
                    eventId[0]++;
                    return event.getEventMentions().size() > 1 ? 1 : 0;
                }
        ).sum();

        logger.info(String.format("Contains %d non-singleton clusters", numNonSingleton));
    }

    public void annotateGoldStandard(JCas aJCas, List<String> bratAnnotations, List<String> tokenOffsetAnnotations) {
        Map<String, Pair<List<Span>, String>> textBoundId2SpanAndType = new HashMap<>();

        List<String> eventIds = new ArrayList<>();
        List<String> eventTextBounds = new ArrayList<>();

        ArrayListMultimap<String, Attribute> id2Attribute = ArrayListMultimap.create();
        List<Relation> relations = new ArrayList<>();

        List<Span> tokenOffsets = new ArrayList<>();
        for (String line : Iterables.skip(tokenOffsetAnnotations, 1)) {
            String[] parts = line.trim().split("\t");
            int tokenBegin = Integer.parseInt(parts[tokenOffsetBeginFieldNumber]);
            int tokenEnd = Integer.parseInt(parts[tokenOffsetEndFieldNumber]);
            tokenOffsets.add(Span.of(tokenBegin, tokenEnd + 1));
        }

        for (String line : bratAnnotations) {
            String[] parts = line.trim().split("\t");
            String annoId = parts[0];
            if (annoId.startsWith(BratConstants.textBoundPrefix)) {
                textBoundId2SpanAndType.put(annoId, getSpanAndType(parts[1]));
            } else if (annoId.startsWith(BratConstants.eventPrefix)) {
                eventIds.add(annoId);
                eventTextBounds.add(parts[1].split(":")[1]);
            } else if (annoId.startsWith(BratConstants.attributePrefix)) {
                Attribute attribute = new Attribute(line);
                id2Attribute.put(attribute.attributeHost, attribute);
            } else if (annoId.startsWith(BratConstants.relationPrefix)) {
                Relation relation = new Relation(line);
                relations.add(relation);
            }
        }

        ArrayListMultimap<String, Span> textBoundId2TokenSpan = ArrayListMultimap.create();
        for (Span tokenSpan : tokenOffsets) {
            for (Map.Entry<String, Pair<List<Span>, String>> textBoundById : textBoundId2SpanAndType.entrySet()) {
                String annoId = textBoundById.getKey();
                // it should be implicitly ensured here that the number of tokenSpan is the same as textBoundSpan
                textBoundById.getValue().getValue0().stream().filter(textBoundSpan -> textBoundSpan.covers(tokenSpan)
                        || tokenSpan.covers(textBoundSpan)).forEach(textBoundSpan -> {
                    // it should be implicitly ensured here that the number of tokenSpan is the same as textBoundSpan
                    textBoundId2TokenSpan.put(annoId, tokenSpan);
                });
            }
        }

        Map<String, EventMention> id2Mentions = annotateMentionAttribute(aJCas, eventIds, eventTextBounds,
                textBoundId2SpanAndType, id2Attribute, textBoundId2TokenSpan);
        annotateRelations(aJCas, relations, id2Mentions);
    }


    public class Attribute {
        String attributeId;
        String attributeName;
        String attributeValue;
        String attributeHost;

        public Attribute(String attributeLine) {
            parseAttribute(attributeLine);
        }

        private void parseAttribute(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            attributeId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            attributeName = attributeFields[0];
            attributeHost = attributeFields[1];
            attributeValue = attributeFields[2];
        }
    }

    public class Relation {
        String relationId;
        String relationName;
        String arg1Name;
        String arg1Id;
        String arg2Name;
        String arg2Id;

        public Relation(String attributeLine) {
            parseRelation(attributeLine);
        }

        private void parseRelation(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            relationId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            relationName = attributeFields[0];
            String[] arg1 = attributeFields[1].split(":");
            String[] arg2 = attributeFields[2].split(":");

            arg1Name = arg1[0];
            arg1Id = arg1[1];

            arg2Name = arg2[0];
            arg2Id = arg2[1];
        }

    }

    private Pair<List<Span>, String> getSpanAndType(String spanText) {
        String[] typeAndSpan = spanText.split(" ", 2);
        String type = typeAndSpan[0];
        String[] spanStrs = typeAndSpan[1].split(";");

        List<Span> spans = new ArrayList<>();
        for (String spanStr : spanStrs) {
            String[] spanTexts = spanStr.split(" ");
            spans.add(Span.of(Integer.parseInt(spanTexts[0]), Integer.parseInt(spanTexts[1])));
        }
        return Pair.with(spans, type);
    }


    public static void main(String[] args) throws UIMAException, IOException {
        logger.info(COMPONENT_ID + " started");
        String parentDir = "data/brat_event";
        String sourceTextDir = parentDir + "/LDC2014E121/source";
        String tokenOffsetDir = parentDir + "/LDC2014E121/token_offset";
        String annotationDir = parentDir + "/LDC2014E121/annotation";
        String baseDir = "gold_annotated";

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription
                (paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, BratEventGoldStandardAnnotator.defaultTextFileNameSuffix);

        AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
                BratEventGoldStandardAnnotator.class, typeSystemDescription,
                BratEventGoldStandardAnnotator.PARAM_ANNOTATION_DIR, annotationDir,
                BratEventGoldStandardAnnotator.PARAM_TOKENIZATION_MAP_DIR, tokenOffsetDir
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentDir, baseDir, null, null
        );

        SimplePipeline.runPipeline(reader, engine, writer);
        System.out.println(COMPONENT_ID + " successfully completed.");
    }
}
