package edu.cmu.cs.lti.script.model;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang.builder.CompareToBuilder;

/**
 * Although this is not so different from MooneyEventRepre, a new class is used to
 * differentiate because this would take arbitrary thing for arguments, while
 * MooneyEventRepre is initially design to hold only a set of variables
 * <p/>
 * <p/>
 * <p/>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/3/14
 * Time: 3:44 PM
 */
public class LocalEventMentionRepre implements Comparable<LocalEventMentionRepre> {
    private final String mentionHead;

    //Note, this could be null! Means no argument at this position
    private final LocalArgumentRepre[] args;

    public LocalEventMentionRepre(String mentionHead, LocalArgumentRepre... args) {
        this.mentionHead = mentionHead;
        this.args = args;
    }

    public void rewrite(TIntIntMap entityIdRewriteMap) {
        for (LocalArgumentRepre arg : args) {
            arg.setRewrittenId(entityIdRewriteMap.get(arg.getEntityId()));
        }
    }

    public static LocalEventMentionRepre fromEventMention(EventMention mention, TokenAlignmentHelper align) {
        LocalArgumentRepre[] args = new LocalArgumentRepre[3];
        for (EventMentionArgumentLink aLink : UimaConvenience.convertFSListToList(mention.getArguments(), EventMentionArgumentLink.class)) {
            String argumentRole = aLink.getArgumentRole();
            if (KmTargetConstants.targetArguments.containsKey(argumentRole)) {
                int slotId = KmTargetConstants.argMarkerToSlotIndex(KmTargetConstants.targetArguments.get(argumentRole));
                int entityId = UimaAnnotationUtils.entityIdToInteger(aLink.getArgument().getReferingEntity().getId());
                LocalArgumentRepre arg = new LocalArgumentRepre(entityId, aLink.getArgument().getHead().getLemma());
                args[slotId] = arg;
            }
        }
        return new LocalEventMentionRepre(align.getLowercaseWordLemma(mention.getHeadWord()), args);
    }

    public static LocalEventMentionRepre rewriteUsingCandidateMention(LocalEventMentionRepre realMention, MooneyEventRepre candidateMention) {
        TIntIntMap rewrittenId2EntityId = new TIntIntHashMap();

        for (int slotId = 0; slotId < realMention.getNumArgs(); slotId++) {
            LocalArgumentRepre arg = realMention.getArg(slotId);
            if (arg != null) {
                rewrittenId2EntityId.put(arg.getRewrittenId(), arg.getEntityId());
            }
        }

        LocalArgumentRepre[] args = new LocalArgumentRepre[3];
        for (int slotId = 0; slotId < candidateMention.getAllArguments().length; slotId++) {
            int rewrittenArgumentId = candidateMention.getAllArguments()[slotId];
            if (rewrittenArgumentId == KmTargetConstants.nullArgMarker) {

            } else if (rewrittenArgumentId == KmTargetConstants.otherMarker) {
                //-1 actually means other here
                LocalArgumentRepre arg = new LocalArgumentRepre(-1, rewrittenArgumentId);
                args[slotId] = arg;
            } else {
                int entityId = rewrittenId2EntityId.get(rewrittenArgumentId);
                LocalArgumentRepre arg = new LocalArgumentRepre(entityId, rewrittenArgumentId);
                args[slotId] = arg;
            }
        }

        return new LocalEventMentionRepre(candidateMention.getPredicate(), args);
    }


    public static LocalEventMentionRepre fromMooneyMention(MooneyEventRepre mention) {
        LocalArgumentRepre[] args = new LocalArgumentRepre[3];
        for (int slotId = 0; slotId < mention.getAllArguments().length; slotId++) {
            int rewriteArgumentId = mention.getAllArguments()[slotId];
            if (rewriteArgumentId != KmTargetConstants.nullArgMarker) {
                LocalArgumentRepre arg = new LocalArgumentRepre(-1, LocalArgumentRepre.UNKNOWN_HEAD, rewriteArgumentId, false);
                arg.setRewrittenId(rewriteArgumentId);
                args[slotId] = arg;
            }
        }
        return new LocalEventMentionRepre(mention.getPredicate(), args);
    }

    public MooneyEventRepre toMooneyMention() {
        return new MooneyEventRepre(mentionHead, args[0] == null ? KmTargetConstants.nullArgMarker : args[0].getRewrittenId(),
                args[1] == null ? KmTargetConstants.nullArgMarker : args[1].getRewrittenId(),
                args[2] == null ? KmTargetConstants.nullArgMarker : args[2].getRewrittenId());
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

    @Override
    /**
     * The comparison for this class is not important, just easy
     */
    public int compareTo(LocalEventMentionRepre o) {
        if (this == o) return 0;
        return new CompareToBuilder().append(this.mentionHead, o.mentionHead).toComparison();
    }
}
