package edu.cmu.cs.lti.cds.model;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.cds.clustering.EntityClusterManager;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.cas.FSList;

import java.util.*;

public class EntityCluster {
    private Set<String> entityIds = new LinkedHashSet<String>();

    private Date firstSeen;

    private Date lastSeen;

    private int maxUnseenDate = 10;

    // Set of representative mentions for cluster
    private List<String> mentionHeads = new ArrayList<String>();

    private ArrayListMultimap<String, String> eventMentions = ArrayListMultimap.create();

    private String clusterType;

    public EntityCluster(String entityId, Entity entity, Date date, String clusterType) {
        String headMentionStr = EntityClusterManager.getRepresentativeStr(entity);
        // System.out.println("Adding head " + mentionHead);
        this.mentionHeads.add(headMentionStr);
        this.clusterType = clusterType;
        this.entityIds.add(entityId);
        this.firstSeen = date;
        this.lastSeen = date;
    }

    public String getClusterType() {
        return clusterType;
    }

    public Set<String> getEntityIds() {
        return entityIds;
    }

    public List<String> getMentionHeads() {
        return mentionHeads;
    }

    public ArrayListMultimap<String, String> getEventMentions() {
        return eventMentions;
    }

    public void addNewEntity(String id, Date lastSeen, String mentionHead) {
        this.entityIds.add(id);
        if (lastSeen != null) {
            this.lastSeen = lastSeen;
        }
        this.mentionHeads.add(mentionHead);
        System.out.println("Adding new entity to cluster: " + mentionHead);
    }

    public void addNewEvents(EventMention eventMention, String role) {
        this.eventMentions.put(role, getEventMentionSummary(eventMention));
    }

    private String getEventMentionSummary(EventMention evm) {
        StringBuilder b = new StringBuilder();
        b.append(evm.getCoveredText());
        FSList arguments = evm.getArguments();
        System.err.println("Event mention is "+evm.getCoveredText());

        if (arguments != null) {
            System.err.println("Number of arguments related to this mention "+FSCollectionFactory.create(arguments, EventMentionArgumentLink.class).size());
            int i = 0;
            for (EventMentionArgumentLink argumentLink : FSCollectionFactory.create(arguments, EventMentionArgumentLink.class)) {
                b.append("\n\t\t"+i+" ");
                i++;
                b.append(argumentLink.getArgumentRole()).append(" - ").append(argumentLink.getArgument().getCoveredText());
            }
        }
        return b.toString();
    }

    public void addNewEvents(ArrayListMultimap<String, EventMention> eventMentions) {
        for (String role : eventMentions.keySet()) {
            List<EventMention> mentionList = eventMentions.get(role);
            for (EventMention m : mentionList) {
                addNewEvents(m, role);
            }
        }
    }

    public boolean checkExpire(Date currentDate) {
        if (lastSeen == null) {
            return true;
        }
        long diff = currentDate.getTime() - lastSeen.getTime();
        if (diff / (24 * 60 * 60 * 1000) > maxUnseenDate) {
            return true;
        }
        return false;
    }

    public Date getFirstSeen() {
        return firstSeen;
    }

    public Date getLastSeen() {
        return lastSeen;
    }
}
