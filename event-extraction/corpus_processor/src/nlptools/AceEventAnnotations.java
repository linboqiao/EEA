package nlptools;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import edu.stanford.nlp.ie.machinereading.domains.ace.AceReader;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import helper.Entity;
import helper.Event;
import helper.SentEvents;
import helper.Utils;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class AceEventAnnotations {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: corpusHome outputFolder filePrefix");
            System.exit(0);
        }
        String corpusHome = args[0];
        String outputFilename = args[1];
        String prefix = args[2];
        BufferedWriter tokensWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".tokens.txt"));
        BufferedWriter lemmasWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".lemmas.txt"));
        BufferedWriter posWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".pos.txt"));
        BufferedWriter nerWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".ner.txt"));
        BufferedWriter depWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".dep.txt"));
        BufferedWriter eventWriter = new BufferedWriter(new FileWriter(outputFilename + prefix + ".event.txt"));

        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        AceReader r = new AceReader(new StanfordCoreNLP(props), false);
        Annotation docs = r.parse(corpusHome);
        Gson gson = new Gson();
        int sentId = 0;
        for (CoreMap sentence: docs.get(CoreAnnotations.SentencesAnnotation.class)) {

            Sentence sent = new Sentence(sentence.toString());

            List<String> tokens = sent.words();
            List<String> lemmas = sent.lemmas();
            List<String> posTags = sent.posTags();
            List<String> nerTags = sent.nerTags();
            List<String> deps = Utils.getDeps(sent, tokens.size());
            if (deps == null) break;
            // Writing to file
            tokensWriter.write(Joiner.on(' ').join(tokens) + '\n');
            lemmasWriter.write(Joiner.on(' ').join(lemmas) + '\n');
            posWriter.write(Joiner.on(' ').join(posTags) + '\n');
            nerWriter.write(Joiner.on(' ').join(nerTags) + '\n');
            depWriter.write(Joiner.on(' ').join(deps) + '\n');


            List<EventMention> evs = sentence.get(MachineReadingAnnotations.EventMentionsAnnotation.class);
            if (evs != null) {
                SentEvents events = new SentEvents();
                events.sentId = sentId;
                for (EventMention ev: evs) {
                    Event event = new Event();
                    event.type = ev.getType();
                    event.subType = ev.getSubType();
                    event.start = ev.getExtentTokenStart();
                    event.end = ev.getExtentTokenEnd();
                    event.objectId = ev.getObjectId();
                    event.args = new ArrayList<>();
                    for (EntityMention em : ev.getEntityMentionArgs()) {
                        Entity entity = new Entity();
                        entity.type = em.getType();
                        entity.subType = em.getSubType();
                        entity.mentionType = em.getMentionType();
                        entity.hstart = em.getHeadTokenStart();
                        entity.hend = em.getHeadTokenEnd();
                        entity.estart = em.getExtentTokenStart();
                        entity.eend = em.getExtentTokenEnd();
                        entity.value = em.getValue();
                        entity.objectId = em.getObjectId();
                        entity.corefId = em.getCorefID();
                        event.args.add(entity);
                    }
                    events.events.add(event);
                }
                eventWriter.write(gson.toJson(events) + '\n');
            }
            sentId ++;
            System.out.println(sentId);
        }
    }
}

