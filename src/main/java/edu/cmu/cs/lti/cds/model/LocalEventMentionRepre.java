package edu.cmu.cs.lti.cds.model;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Although this is not so different from MooneyEventRepre in the form, we need
 * to differentiate because this would take arbitrary thing for argments, while
 * int Mooney representation we restrict a set of variables
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/3/14
 * Time: 3:44 PM
 */
public class LocalEventMentionRepre {
    private String mentionHead;
    //entity id (clustered mentions), head word pair
    private Pair<Integer, String>[] args;

    //TODO: change mention head to an integer
    public LocalEventMentionRepre(String mentionHead, Pair<Integer, String> arg0, Pair<Integer, String> arg1, Pair<Integer, String> arg2) {
        this.mentionHead = mentionHead;
        this.args[0] = arg0;
        this.args[1] = arg1;
        this.args[2] = arg2;
    }

    public LocalEventMentionRepre(String mentionHead, Pair<Integer, String>... args) {
        this.mentionHead = mentionHead;
        this.args = args;
    }

    public static LocalEventMentionRepre fromEventMention(EventMention mention, TokenAlignmentHelper align) {
        TIntIntHashMap evm1Args = new TIntIntHashMap();
        TIntIntHashMap evm1Slots = new TIntIntHashMap();

        Pair<Integer, String>[] args = new Pair[3];

        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();
            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int slotId = KmTargetConstants.targetArguments.get(argumentRole) - KmTargetConstants.anchorArg0Marker;
                args[slotId] = Pair.of(Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId()), aLink.getArgument().getHead().getLemma());
                evm1Slots.put(slotId, KmTargetConstants.otherMarker);
            }
        }

        return new LocalEventMentionRepre(align.getLowercaseWordLemma(mention.getHeadWord()), args);
    }

    public String getMentionHead() {
        return mentionHead;
    }

    public void setMentionHead(String mentionHead) {
        this.mentionHead = mentionHead;
    }

    public Pair<Integer, String> getArg(int i) {
        return args[i];
    }

    public Pair<Integer, String>[] getArgs() {
        return args;
    }

    public int getNumArgs() {
        return args.length;
    }
}
