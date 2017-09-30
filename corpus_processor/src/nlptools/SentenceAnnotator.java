package nlptools;

import com.google.common.base.Joiner;
import edu.stanford.nlp.simple.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class SentenceAnnotator {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: inputFilename outputFileprefix");
        }
        String inputFilename = args[0];
        String outputFilename = args[1];
        BufferedWriter tokensWriter = new BufferedWriter(new FileWriter(outputFilename + ".tokens.txt"));
        BufferedWriter lemmasWriter = new BufferedWriter(new FileWriter(outputFilename + ".lemmas.txt"));
        BufferedWriter posWriter = new BufferedWriter(new FileWriter(outputFilename + ".pos.txt"));
        BufferedWriter nerWriter = new BufferedWriter(new FileWriter(outputFilename + ".ner.txt"));
        BufferedWriter depWriter = new BufferedWriter(new FileWriter(outputFilename + ".dep.txt"));
        BufferedReader br = new BufferedReader(new FileReader(inputFilename));

        String line;
        while ((line = br.readLine()) != null) {
            Sentence sent = new Sentence(line);
            List<String> tokens = sent.words();
            List<String> lemmas = sent.lemmas();
            List<String> posTags = sent.posTags();
            List<String> nerTags = sent.nerTags();
            List<String> deps = new ArrayList<>();
            Boolean depPresent = true;
            for (int i = 0; i < tokens.size(); i++) {
                Optional<Integer> governor = sent.governor(i);
                Optional<String> dependencyLabel = sent.incomingDependencyLabel(i);
                if (governor.isPresent() && dependencyLabel.isPresent()){
                    deps.add(Integer.toString(governor.get() + 1) + '_' + dependencyLabel.get());
                }
                else {
                    depPresent = false;
                    break;
                }
            }
            if (!depPresent) {
                break;
            }
            // Writing to file
            tokensWriter.write(Joiner.on(' ').join(tokens) + '\n');
            lemmasWriter.write(Joiner.on(' ').join(lemmas) + '\n');
            posWriter.write(Joiner.on(' ').join(posTags) + '\n');
            nerWriter.write(Joiner.on(' ').join(nerTags) + '\n');
            depWriter.write(Joiner.on(' ').join(deps) + '\n');
        }
        br.close();
        tokensWriter.close();
        lemmasWriter.close();
        posWriter.close();
        nerWriter.close();
        depWriter.close();
    }
}
