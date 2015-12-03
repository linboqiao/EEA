package edu.cmu.cs.lti.emd.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EventMention;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/27/15
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionTypeUtils {
    public static String TYPE_NAME_JOINER = " ; ";

    /**
     * The same span might be annotated as multiple types, we join them together.
     *
     * @param mentions All the mentions of interested.
     * @return From the mention span to the type.
     */
    public static Map<Span, String> mergeMentionTypes(Collection<EventMention> mentions) {
        ArrayListMultimap<Span, String> mergedMentionTypes = ArrayListMultimap.create();
        for (EventMention mention : mentions) {
            mergedMentionTypes.put(Span.of(mention.getBegin(), mention.getEnd()), mention.getEventType());
        }

        Map<Span, String> mentionWithMergedTypes = new TreeMap<>();

        for (Span span : mergedMentionTypes.keySet()) {
            TreeSet<String> uniqueSortedTypes = new TreeSet<>(mergedMentionTypes.get(span));
            mentionWithMergedTypes.put(span, joinMultipleTypes(uniqueSortedTypes));
        }

        return mentionWithMergedTypes;
    }

    public static String joinMultipleTypes(Iterable<String> types) {
        return Joiner.on(TYPE_NAME_JOINER).join(types);
    }

    public static String[] splitToTmultipleTypes(String joinedType) {
        return joinedType.split(TYPE_NAME_JOINER);
    }

}
