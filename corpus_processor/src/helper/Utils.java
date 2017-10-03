package helper;

import edu.stanford.nlp.simple.Sentence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utils {
    public static List<String> getDeps(Sentence sent, int length) {
        List<String> deps = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Optional<Integer> governor = sent.governor(i);
            Optional<String> dependencyLabel = sent.incomingDependencyLabel(i);
            if (governor.isPresent() && dependencyLabel.isPresent()){
                deps.add(Integer.toString(governor.get() + 1) + '_' + dependencyLabel.get());
            }
            else {
                return null;
            }
        }
        return deps;
    }
}
