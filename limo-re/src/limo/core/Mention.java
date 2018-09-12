package limo.core;

import java.util.ArrayList;
import java.util.Arrays;

import edu.stanford.nlp.trees.Tree;

import limo.core.interfaces.IMention;

/**
 * A mention is an actual occurrence of an entity in the text.
 * It must contain a reference to an entity (which holds the entityType, e.g. PER)
 * and contains an id.
 * 
 * @author Barbara Plank
 *
 */
public class Mention implements IMention, Comparable<Mention> {

	private String id;
	private String type;
	private int extendStart;
	private int extendEnd;
	private String extend;
	private String head;
	private int headStart;
	private int headEnd;

	private ArrayList<Token> tokensHead;

	private Entity entityReference;
	private boolean isInRelation;
	
	//optional
	private String mentionLDCType = "";
	private String mentionRole = "";
	private String mentionReference = "";
	private boolean ignore = false;
	
	
	public Mention(String mentionId, String mentionType, String extend, int extendStart,
			int extendEnd, String head, int headStart, int headEnd, Entity entityOfMention) {
		this.id = mentionId;
		this.type = mentionType;
		this.tokensHead = new ArrayList<Token>();
		this.extendStart = extendStart;
		this.extendEnd = extendEnd;
		this.extend = extend;
		if (head.contains("\n"))
			this.head = head.replaceAll("\n", " ");
		else
			this.head = head;
		this.headStart = headStart;
		this.headEnd = headEnd;
		this.entityReference = entityOfMention;
	}

	
	public Mention(String mentionId, String mentionType) {
		this.id = mentionId;
		this.type = mentionType;
		this.entityReference = null;
	}


	public String getHead() {
		return this.head;
	}

	public String getId() {
		return this.id;
	}

	public String getType() {
		return this.type;
	}

	public void setExtend(String extend) {
		this.extend = extend;	
	}
	
	public void setHead(String head) {
		this.head = head;	
	}

	public String getExtend() {
		return this.extend;
	}

	@Override
	public String toString() {
		return "["+head+" startHead:"+headStart + " endHead:"+headEnd+ " extend: "+ extend+ "]";
	}

	public int getExtendStart() {
		return extendStart;
	}

	public void setExtendStart(int extendStart) {
		this.extendStart = extendStart;
	}

	public int getExtendEnd() {
		return extendEnd;
	}

	public void setExtendEnd(int extendEnd) {
		this.extendEnd = extendEnd;
	}

	public int getHeadStart() {
		return headStart;
	}

	public void setHeadStart(int headStart) {
		this.headStart = headStart;
	}

	public int getHeadEnd() {
		return headEnd;
	}

	public void setHeadEnd(int headEnd) {
		this.headEnd = headEnd;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void addHeadToken(Token token) {
		if (!tokensHead.contains(token))
			this.tokensHead.add(token);
	}
	
	public boolean containsHeadToken(Token token) {
		for (Token t : this.tokensHead) {
			if (t.equals(token))
				return true;
		}
		return false;
	}
	
	public void setHeadTokens(ArrayList<Token> tokensOfMention) {
		this.tokensHead = tokensOfMention;
	}
	
	public Entity getEntityReference() {
		return entityReference;
	}

	public void setEntityReference(Entity entityReference) {
		this.entityReference = entityReference;
	}
	
	/** 
	 * whether mention participates in one or more relations
	 */
	public void setIsInRelation(boolean b) {
		this.isInRelation = b;
	}
	
	public boolean isInRelation() {
		return this.isInRelation;
	}
	
	public int getSentenceId() throws Exception  {
		if (this.tokensHead.size() > 0) {
		return this.tokensHead.get(0).getSentenceReference().getSentenceId();
		} else {
			System.err.println(this.entityReference.getId() + " "+this.getHead());
			throw new Exception("did you call matchTokensAndMentions?");
		}
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((entityReference == null) ? 0 : entityReference.hashCode());
		result = prime * result + ((extend == null) ? 0 : extend.hashCode());
		result = prime * result + extendEnd;
		result = prime * result + extendStart;
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + headEnd;
		result = prime * result + headStart;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((type == null) ? 0 : type.hashCode());
		result = prime * result
				+ ((tokensHead == null) ? 0 : tokensHead.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mention other = (Mention) obj;
		
		//check if mention overlaps with mention1 or mention2 w.r.t tokens
		int[] tokenIds1 = other.getTokenIds();
		int[] tokenIds = this.getTokenIds();
		if (Arrays.equals(tokenIds, tokenIds1))
				return true;
		if (extend == null) {
			if (other.extend != null)
				return false;
		} else if (!extend.equals(other.extend))
			return false;
		if (extendEnd != other.extendEnd)
			return false;
		if (extendStart != other.extendStart)
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (headEnd != other.headEnd)
			return false;
		if (headStart != other.headStart)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (tokensHead == null) {
			if (other.tokensHead != null)
				return false;
		} else if (!tokensHead.equals(other.tokensHead))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public String getMentionLDCType() {
		return mentionLDCType;
	}

	public void setMentionLDCType(String mentionLDCType) {
		this.mentionLDCType = mentionLDCType;
	}

	public String getMentionRole() {
		return mentionRole;
	}

	public void setMentionRole(String mentionRole) {
		this.mentionRole = mentionRole;
	}
	
	public String getMentionReference() {
		return mentionReference;
	}

	public void setMentionReference(String mentionReference) {
		this.mentionReference = mentionReference;
	}


	public int[] getTokenIds() {
		int[] ids = new int[this.tokensHead.size()];
		for (int i=0; i < this.tokensHead.size(); i++) {
			ids[i] = this.tokensHead.get(i).getTokenId();
		}
		return ids;
	}

	// to order mentions as they appear in a sentence
	public int compareTo(Mention other) {
		return ((Integer)this.getHeadStart()).compareTo((Integer)other.getHeadStart());
	}


	public boolean before(Mention mention2) {
		return this.getHeadStart() < mention2.getHeadStart(); 
	}

	//check if terminal (from ParseTree) is part of mention.
	public boolean spansOverTerminal(Tree terminal) {
		String surface = terminal.label().value();
		for (Token t : this.tokensHead) {
			String part = t.getValue();
			if (surface.contains(part))
				return true;
		}
		
		return false;
	}


	public boolean sameHeadToken(Mention mention2) {
		//check if sentence is the same!
		try {
			if (this.getSentenceId() != mention2.getSentenceId())
				return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		int[] tokensMention1 = this.getTokenIds();
		int[] tokensMention2 = mention2.getTokenIds();
		if (tokensMention1.length != tokensMention2.length)
			return false;
		for (int i=0; i < tokensMention1.length; i++) {
			if (tokensMention1[i] != tokensMention2[i])
				return false;
		}
		return true;
	}

	
	/***
	 * Check if two tokens (represented by tokenIds integer array) overlap
	 * @param tokenIds1
	 * @param tokenIds2
	 * @return true if some tokens are the same
	 */
	public boolean overlapping(Mention other) {
		int[] tokenIds1 = this.getTokenIds();
		int[] tokenIds2 = other.getTokenIds();
		for (int i=0; i<tokenIds1.length;i++) {
			for (int j=0; j<tokenIds2.length; j++) {
				if (tokenIds1[i]==tokenIds2[j])
					return true;
			}
				
		}
		return false;
	}

	/**
	 * set this to true if a mention is annotated outside annotation text (outside <TEXT> in ace)
	 * @param value
	 */
	public void setToIgnore(boolean value) {
		this.ignore = value;
	}
	public boolean isToIgnore() {
		return this.ignore;
	}
	
}
