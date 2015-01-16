package edu.cmu.cs.lti.script.model;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.TIntIntMap;

/**
 * Although this is not so different from MooneyEventRepre, a new class is used to
 * differentiate because this would take arbitrary thing for arguments, while
 * MooneyEventRepre is initially design to hold only a set of variables
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/3/14
 * Time: 3:44 PM
 */
public class LocalEventMentionRepre {
    private final String mentionHead;

    //Note, this could be null! Means no argument at this position
    private final LocalArgumentRepre[] args;

    public LocalEventMentionRepre(String mentionHead, LocalArgumentRepre... args) {
        this.mentionHead = mentionHead;
        this.args = args;
    }

    public void rewrite(TIntIntMap entityIdRewriteMap) {
        for (LocalArgumentRepre arg : args) {
            arg.setRewritedId(entityIdRewriteMap.get(arg.getEntityId()));
        }
    }

    public static LocalEventMentionRepre fromEventMention(EventMention mention, TokenAlignmentHelper align) {
        LocalArgumentRepre[] args = new LocalArgumentRepre[3];
        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();
            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int slotId = KmTargetConstants.targetArguments.get(argumentRole) - KmTargetConstants.anchorArg0Marker;
                int entityId = Utils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                LocalArgumentRepre arg = new LocalArgumentRepre(entityId, aLink.getArgument().getHead().getLemma());
                args[slotId] = arg;
            }
        }
        return new LocalEventMentionRepre(align.getLowercaseWordLemma(mention.getHeadWord()), args);
    }

    public static LocalEventMentionRepre fromMooneyMention(MooneyEventRepre mention) {
        LocalArgumentRepre[] args = new LocalArgumentRepre[3];
        for (int slotId = 0; slotId < mention.getAllArguments().length; slotId++) {
            int rewriteArgumentId = mention.getAllArguments()[slotId];
            if (rewriteArgumentId != KmTargetConstants.nullArgMarker) {
                LocalArgumentRepre arg = new LocalArgumentRepre(-1, LocalArgumentRepre.UNKNOWN_HEAD, rewriteArgumentId, false);
                arg.setRewritedId(rewriteArgumentId);
                args[slotId] = arg;
            }
        }
        return new LocalEventMentionRepre(mention.getPredicate(), args);
    }

    public MooneyEventRepre toMooneyMention() {
        return new MooneyEventRepre(mentionHead, args[0] == null ? KmTargetConstants.nullArgMarker : args[0].getRewritedId(),
                args[1] == null ? KmTargetConstants.nullArgMarker : args[1].getRewritedId(),
                args[2] == null ? KmTargetConstants.nullArgMarker : args[2].getRewritedId());
    }


    public String getMentionHead() {
        return mentionHead;
    }

    public LocalArgumentRepre getArg(int i) {
        return args[i];
    }

    public LocalArgumentRepre[] getArgs() {
        return args;
    }

    public int getNumArgs() {
        return args.length;
    }

    /**
     * To test whether a prediction is correct
     *
     * @param repre
     * @return
     */
    public boolean mooneyMatch(LocalEventMentionRepre repre) {
        if (!mentionHead.equals(repre.getMentionHead())) {
            return false;
        }

        for (int i = 0; i < this.getNumArgs(); i++) {
            if (args[i] == null) {
                if (repre.getArg(i) != null) {
                    return false;
                }
            } else {
                if (repre.getArg(i) == null) {
                    return false;
                } else {
                    if (!args[i].mooneyMatch(repre.getArg(i))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mentionHead).append(" : ");
        String sep = "";
        for (int i = 0; i < args.length; i++) {
            sb.append(sep);
            LocalArgumentRepre arg = args[i];
            if (arg != null) {
                sb.append("arg").append(i).append(" ").append(arg.toString());
            } else {
                sb.append("arg").append(i).append(" null");
            }
            sep = ",";
        }

        return sb.toString();
    }
}
