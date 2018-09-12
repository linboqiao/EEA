package limo.core.trees.dependency;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import limo.cluster.BrownWordCluster;
import limo.core.trees.AbstractTree;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure.FromDependenciesFactory;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.semgraph.SemanticGraphFactory;

/***
 *  A DependencyInstance
 *  
 *  Inspired by MSTparser. Extended to get various kind of substructures
 *  
 *  @author Barbara Plank
 *	
 */
public class DependencyInstance extends AbstractTree {

    //public FeatureVector fv;
    //public String actParseTree;

    // The various data types. 
    // ID FORM LEMMA COURSE-POS FINE-POS  FEATURES HEAD DEPREL PHEAD PDEPREL
    //
    // We ignore PHEAD and PDEPREL for now. 

    // FORM: the forms - usually words, like "thought"
    public String[] forms;

    // LEMMA: the lemmas, or stems, e.g. "think"
    public String[] lemmas;

    // COURSE-POS: the course part-of-speech tags, e.g."V"
    public String[] cpostags;

    // FINE-POS: the fine-grained part-of-speech tags, e.g."VBD"
    public String[] postags;

    // FEATURES: some features associated with the elements separated by "|", e.g. "PAST|3P"
    public String[][] feats;

    // HEAD: the IDs of the heads for each element
    public int[] heads;

    // DEPREL: the dependency relations, e.g. "SUBJ"
    public String[] deprels;

	private Double classificationScore = null;

	private GrammaticalStructure grammaticalStructure;

	private SemanticGraph semGraph; //original graph entire sentence
	
	private double score;
	
    public DependencyInstance() {}

	public DependencyInstance(String[] forms, String[] lemmas,
			String[] cpostags, String[] postags, String[][] feats,
			String[] labs, int[] heads, List<List<String>> tokenFields) {
		this.forms = forms;
		this.postags = postags;
		this.deprels = labs;
		this.heads = heads;
		this.lemmas = lemmas;
		this.cpostags = cpostags;
		this.feats = feats;

		boolean collapsed = false;
		this.grammaticalStructure = createGrammaticalStructure(tokenFields);
		this.semGraph = createSemanticGraph(this.grammaticalStructure, collapsed);
		
	}

	public DependencyInstance(String[] forms, String[] lemmas,
			String[] cpostags, String[] postags, String[][] feats,
			String[] labs, int[] heads, List<List<String>> tokenFields, double score) {
		this(forms, lemmas, cpostags, postags, feats, labs, heads, tokenFields);
		this.score = score;
	}
   
    public int length () {
    	return forms.length;
    }

    public String toString () {
		StringBuffer sb = new StringBuffer();
		sb.append(Arrays.toString(forms)).append("\n");
		return sb.toString();
    }


    private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeObject(forms);
		out.writeObject(lemmas);
		out.writeObject(cpostags);
		out.writeObject(postags);
		out.writeObject(heads);
		out.writeObject(deprels);
		out.writeObject(feats);
    }


    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		forms = (String[]) in.readObject();
		lemmas = (String[]) in.readObject();
		cpostags = (String[]) in.readObject();
		postags = (String[]) in.readObject();
		heads = (int[]) in.readObject();
		deprels = (String[]) in.readObject();
		feats = (String[][]) in.readObject();
    }

    /***
     * add score to dependency instance
     * (weight from text classifier used for instance weighting)
     * @param nextScore
     */
	public void setClassificationScore(Double score) {
		this.classificationScore = score;
	}
	
	public Double getClassificationScore() {
		return this.classificationScore;
	}


	/**
	 * Get the grammatical structure
	 * @return
	 */
	public GrammaticalStructure getGrammaticalStructure() {
		return this.grammaticalStructure;
	}
	

	/***
	 * Get the semantic structure
	 * @return
	 */
	public SemanticGraph getSemanticGraph() {
		//return this.semGraph; make a copy!
		return createSemanticGraph(this.grammaticalStructure, false);
	}
	
	/***
	 * Get the semantic structure
	 * @return
	 */
	public SemanticGraph getCollapsedSemanticGraph() {
		return createSemanticGraph(this.grammaticalStructure, true);
	}
	
	/***
	 * Get nested tree 
	 * Alters the semGraphAltered
	 * @param outputType 
	 * @param includePos 
	 * @return
	 */
	public String getNestedTree(SemanticGraph graph, Output outputType) {
		return this.traverseSemanticGraph(graph,outputType);
	}
	
	
	public String getNestedTree(SemanticGraph graph) {
		return this.traverseSemanticGraph(graph,Output.WORD_BELOW_DEP);
	}
	
	

	/***
	 * Returns the number of edges 
	 * @return
	 */
	public int getNumTypedDependencies() {
		return this.grammaticalStructure.typedDependencies().size();
	}
	
	/***
	 * Returns the number of edges in the collapsed representation
	 * @return
	 */
	public int getNumTypedDependenciesCollapsed() {
		return this.grammaticalStructure.typedDependenciesCollapsed().size();
	}
	
	

	/**
	 * create grammatical structure object 
	 * @param tokenFields
	 * @return GrammaticalStructure
	 */
	private GrammaticalStructure createGrammaticalStructure(List<List<String>> tokenFields) {
		return GrammaticalStructure.buildCoNNLXGrammaticStructure(tokenFields, EnglishGrammaticalRelations.shortNameToGRel, new FromDependenciesFactory());
	}
	
	private SemanticGraph createSemanticGraph(GrammaticalStructure gs,boolean collapsed) {
		try {
			if (!collapsed)
				return SemanticGraphFactory.makeFromTree(gs, "",0); //dummy docId and sentenceId
			else {
				Collection<TypedDependency> reducedDeps = gs.typedDependenciesCollapsed(); 
				return new SemanticGraph(reducedDeps,null);
			}	
		} catch (NullPointerException e) {
			System.err.println(e);
			
			System.err.println("problem creating semantic graph! ");
			for (TypedDependency d : gs.typedDependencies())
				System.err.println(d);
			return null;
		}
	}

        public enum Output {WORD, POS_OVER_WORD, POS,DEP, WORD_BELOW_DEP,POS_BELOW_DEP, POS_AND_WORD_BELOW_DEP, WORD_TARGET};
	

	/**
	 * Returns a tree by traversing graph
	 * @param semgraph
	 * @return
	 */
	private String traverseSemanticGraph(SemanticGraph semgraph, Output outputformat) {
		if (semgraph == null)
			return "(EMPTY)";
		StringBuilder out = new StringBuilder();
		HashSet<IndexedWord> used = new HashSet<IndexedWord>(); //visited tokens
		//one root only
		if (semgraph.getRoots().size()==1) {
			
			IndexedWord root = semgraph.getFirstRoot(); //root node
		
			traverseGraph(semgraph,root,used,outputformat, out);
		
			
		} else {
			//multiple roots
			out.append("(TOP "); //dummy top around all roots
			for (IndexedWord root: semgraph.getRoots()) {
				//traverse tree for every root
					
				traverseGraph(semgraph,root,used, outputformat, out);
				
			}
			out.append(")"); //close TOP
		}
		return out.toString();
	}
	
	/***
	 * This method specifies how the information should be structured
	 * @param node
	 * @param graph
	 * @param used
	 * @param outputformat 
	 * @return 
	 */
	private String getOutput(IndexedWord node, SemanticGraph graph, HashSet<IndexedWord> used, Output outputformat) {
		if (graph.size()==0)
			return "ROOT ("+ node.value() + ")"; //just 1 node
		IndexedWord tmp_parentOfWord = graph.getParent(node);
		String output = "";
		if (tmp_parentOfWord==null) { //then it's the root, add that
			//output = "(ROOT (" + node.tag() + " ("+ node.value() + "))";  
			//output = "ROOT ("+ node.value() + ")";
			
			if (outputformat == Output.WORD_BELOW_DEP) 
				output = "Root"+getTarget(node)+ " ("+ node.value() + ")";
			else if (outputformat == Output.POS_BELOW_DEP) 
				output = "Root"+getTarget(node)+ " ("+ node.tag() + ")";
			else if (outputformat == Output.POS_AND_WORD_BELOW_DEP) 
				output = "Root"+getTarget(node)+ " ("+ node.tag() + " (" +node.value() + "))";
			else if  (outputformat == Output.DEP) 
				output = "Root"+getTarget(node);

			    
		}
		else { //not root node
			IndexedWord parentOfWord = graph.getNodeByIndex(tmp_parentOfWord.index());
			SemanticGraphEdge edge = graph.getEdge(parentOfWord, node);
			GrammaticalRelation grel = edge.getRelation();
			//output = "("+grel.toString() +" (" + node.tag() + " ("+ node.value() + "))";  
			//output = grel.toString()+getTarget(node) +" ("+ node.value()+")";
			if (outputformat == Output.WORD_BELOW_DEP) 
				output = grel.toString()+getTarget(node)+ " ("+ node.value() + ")";
			else if (outputformat == Output.POS_BELOW_DEP) 
				output = "Root"+getTarget(node)+ " ("+ node.tag() + ")";
			else if (outputformat == Output.POS_AND_WORD_BELOW_DEP) 
				output = grel.toString()+getTarget(node) + " ("+ node.tag() + " (" +node.value() + "))";
			else if  (outputformat == Output.DEP) 
			    	output =  grel.toString()+getTarget(node);

		}
		String space = (graph.outDegree(node)>0) ? " " : ""; 
		return output + space;
	}
/*	private String getOutput(IndexedWord node, SemanticGraph graph, HashSet<IndexedWord> used) {
		
		IndexedWord tmp_parentOfWord = graph.getParent(node);
		
		if (tmp_parentOfWord==null) { //then it's the root, add that
			if (!used.contains(node)) {
				used.add(node); //visited
				String output;
				if (node.category()!= null) {//if we have label information
					//new:
					if (!getTarget(node).equals(""))
						output =  "(ROOT"+" ("+getTarget(node)+getIfPathNode(node)+ " ("+node.value() +")("+node.tag()+"))"; //attach above word
					else
						output =  "(ROOT"+" ("+node.value() +")("+node.tag()+")"; //attach above word
					//v1:
					//output = "(ROOT"+getTarget(node)+getIfPathNode(node)+" (" + node.value() +" ("+node.tag()+"))"; //attach to deprel
					output = node.value()+ " ";
					//v2:
					//String target = getTarget(node) != "" ? "("+getTarget(node)+") " : "";
					//output = "(ROOT "+target+"(" + node.value() + ")"; //below deprel
					//v3: including pos above word
					//String target = getTarget(node) != "" ? "("+getTarget(node)+") " : "";
					//output = "(ROOT "+target+"(" + node.tag()+ " ("+node.value() + "))"; //below deprel
					//v4: is v1 with pos above words
					//output = "(ROOT"+getTarget(node)+" ("+node.tag() + " (" + node.value() + "))"; //attach to deprel
				}
				else
					//output = "(ROOT"+getIfPathNode(node)+" (" + node.value() +" ("+node.tag()+"))";
					//output = "(ROOT"+" (" + node.value() +" ("+node.tag()+"))";
					//output = "(ROOT"+getTarget(node)+" ("+node.tag() + " (" + node.value() + "))"; //attach to deprel
					output = node.value() + " ";
				//if (graph.outDegree(node)>0)
			    //	output = output + " ";
				return output;
			} else {
				// we already have it, just add closing parenthesis
				//return ")";
				return "STOP";
				
			}
		} 
		else { //not root node
			IndexedWord parentOfWord = graph.getNodeByIndex(tmp_parentOfWord.index());
			SemanticGraphEdge edge = graph.getEdge(parentOfWord, node);
			GrammaticalRelation grel = edge.getRelation();
			if (!used.contains(node)) {
				used.add(node); //visited
				//
				//String output;
//				if (!getTarget(node).equals(""))
//					output =  "("+grel.toString()+" ("+getTarget(node)+getIfPathNode(node)+ " ("+node.value() +")("+node.tag()+"))"; //attach above word
//				else 
//					output =  "("+grel.toString()+" ("+node.value() +")("+node.tag()+")"; //attach above word
//				//String output = "("+grel.toString() +getTarget(node)+getIfPathNode(node)+" (" + node.value()+" ("+node.tag()+"))";  //need to use toString() to have specific for collapsed deps
				//v2:
				//String target = getTarget(node) != "" ? "("+getTarget(node)+") " : "";
				//String output = "("+grel.toString() +" "+ target + "(" + node.value() + ")";  //below deprel
				//v3:
				//String target = getTarget(node) != "" ? "("+getTarget(node)+") " : "";
				//String output = "("+grel.toString() +" "+ target + "(" + node.tag() + " ("+ node.value() + "))";  //below deprel
				//v4: is v1 with pos above words
				//String output = "("+grel.toString() +getTarget(node) +" (" +node.tag() + " ("+ node.value() + "))";  //need to use toString() to have specific for collapsed deps
				String output = node.value() + " ";
				if (graph.outDegree(node)>0)
			    	output = output + " ";
				return output;
			} else {
				// we already have it, just add closing parenthesis
				//return ")";
				return "<> ";
			}
		}
	}*/
	
	// node.category() contains a list of key-value pairs all separated by space; extract value for key "target"
	private String getTarget(IndexedWord node) {
		String cat = node.category();
		if (cat==null) 
			return "";
		else {
			String[] fields = cat.split(" ");
			for (int i=0; i < fields.length; i+=2) {
				if (fields[i].equals("target"))
					return delim+fields[i+1];
				if (fields[i].equals("non-target"))
					return delim+fields[i+1];
			}
		}
		return "";
	}
	
	private String getIfPathNode(IndexedWord node) {
		return "";
//		String cat = node.category();
//		if (cat==null) 
//			return delim+"O";
//		else {
//			String[] fields = cat.split(" ");
//			for (int i=0; i < fields.length; i+=2) {
//				if (fields[i].equals("pathNode"))
//					return delim+fields[i+1];
//				if (fields[i].equals("pathDep"))
//					return delim+fields[i+1];
//			}
//		}
//		//return "";
//		return delim+"O";
	}

	//continue to traverse graph starting from current node, append to out
//	private void traverseGraph(SemanticGraph semgraph, IndexedWord node, HashSet<IndexedWord> used, Output outputformat, StringBuilder out) {
//		System.out.println();
//		out.append(getOutput(node, semgraph, used)); //output node
//		
//		for (SemanticGraphEdge depcy : semgraph.getOutEdgesSorted(node)) {
//
//			IndexedWord tmp_dep = depcy.getDependent(); //this sometimes looses the augmented labels, thus hack: get node by index
//			IndexedWord dep = semgraph.getNodeByIndex(tmp_dep.index());
//			
//			out.append(getOutput(dep, semgraph, used)); //output dependent
//					  
//		    //add children
//			List<SemanticGraphEdge> edges =  semgraph.getOutEdgesSorted(dep);
//			
//		    for (SemanticGraphEdge depChild : edges) {
//		    	
//		    	IndexedWord tmp_child = depChild.getDependent(); //here again hack
//		    	IndexedWord child = semgraph.getNodeByIndex(tmp_child.index());
//		    	
//		    	traverseGraph(semgraph, child, used,outputformat,out);
//		    	
//		    } //add Children
//		    out.append(getOutput(dep, semgraph, used)); //close dependent
//		}
//		out.append(getOutput(node, semgraph, used)); //close node
//	}
//		 
	
	
/*	preorder(node)
	  if node == null then return
	  visit(node)
	  preorder(node.left) 
	  preorder(node.right)
*/
	  
	private void traverseGraph(SemanticGraph semgraph, IndexedWord node, HashSet<IndexedWord> used, Output outputformat, StringBuilder out) {
		if (node == null)
			return;
		//System.out.println(">> "+node.value());
		out.append("(");
		out.append(getOutput(node, semgraph, used, outputformat)); //output node
	
		for (SemanticGraphEdge edge : semgraph.getOutEdgesSorted(node)) {

			IndexedWord tmp_dep = edge.getDependent(); //this sometimes looses the augmented labels, thus hack: get node by index
			IndexedWord dep = semgraph.getNodeByIndex(tmp_dep.index());
			
			List<SemanticGraphEdge> left = new ArrayList<SemanticGraphEdge>();
			List<SemanticGraphEdge> right = new ArrayList<SemanticGraphEdge>();
			
			if (dep.index() < node.index())
				left.add(edge);
	    	else
	    		right.add(edge);
			
		
		    for (SemanticGraphEdge e : left) {
		    	IndexedWord tmp_child = e.getDependent(); //here again hack
		    	IndexedWord child = semgraph.getNodeByIndex(tmp_child.index());
		    	
		    	traverseGraph(semgraph, child, used,outputformat,out);
		    	
		    }
		    
		    for (SemanticGraphEdge e : right) {
		    	IndexedWord tmp_child = e.getDependent(); //here again hack
		    	IndexedWord child = semgraph.getNodeByIndex(tmp_child.index());
		    	traverseGraph(semgraph, child, used,outputformat,out);
		    	
		    }
		    
		}
		out.append(")");
	}
		  
	

//	/**
//	 * Returns a tree by traversing graph
//	 * @param semgraph
//	 * @return
//	 */
//	private String traverseSemanticGraph(SemanticGraph semgraph,boolean isGRCT, boolean isDWT, Output outputformat) {
//		//if (semgraph.vertexSet().isEmpty())
//		if (semgraph == null)
//			return "(EMPTY)";
//		StringBuilder out = new StringBuilder();
//		HashSet<IndexedWord> used = new HashSet<IndexedWord>();
//		//one root only
//		if (semgraph.getRoots().size()==1) {
//			IndexedWord root = semgraph.getFirstRoot(); //verb			
//			if (isGRCT)
//				//is GRCT
//				out.append("(ROOT ");
//			else if (isDWT) {
//				if (outputformat == Output.WORD) {
//					//attach no root!
//					out.append("("+root.value()+" ");
//				} 
//				else if (outputformat == Output.DEP)
//					out.append("(ROOT ");
//				else
//					out.append("("+root.value()+" "); //default
//			}
//			else
//				if (outputformat == Output.POS_OVER_WORD)
//					out.append("(ROOT (" +root.tag() +" ("+root.value()+" ");
//				else
//					out.append("(ROOT (" +root.value() +" ");
//			
//			traverseGraph(semgraph,root,used,isGRCT, isDWT, outputformat, out);
//			
//			if (!isDWT) out.append(")");//close ROOT
//			if (!isGRCT)
//				out.append(")");
//			if (outputformat == Output.POS_OVER_WORD && isGRCT==false) out.append(")");//close pos
//		} else {
//			//multiple roots
//			out.append("(TOP "); //dummy top around all roots
//			for (IndexedWord root: semgraph.getRoots()) {
//				//traverse tree for every root
//				if (isGRCT)
//					out.append("(ROOT ");
//				else if (isDWT)
//					//attach no root!
//					out.append("("+root.value()+" ");
//				else
//					if (outputformat == Output.POS_OVER_WORD)
//						out.append("(ROOT (" +root.tag() +" ("+root.value()+" ");
//					else
//						out.append("(ROOT (" +root.value() +" ");
//				
//					
//				traverseGraph(semgraph,root,used,isGRCT,isDWT, outputformat, out);
//				out.append(")");//close ROOT
//				if (!isDWT) out.append(")");
//				if (outputformat == Output.POS_OVER_WORD && isGRCT==false) out.append(")");//close pos
//			}
//			out.append(")");
//		}
//		return out.toString();
//	}
//	
//	//continue to traverse graph starting from current node, append to out
//	private void traverseGraph(SemanticGraph semgraph, IndexedWord node, HashSet<IndexedWord> used, boolean isGRCT,  boolean isDWT, Output outputformat, StringBuilder out) {
//	
//		for (SemanticGraphEdge depcy : semgraph.getOutEdgesSorted(node)) {
//			
//			String reln = depcy.getRelation().toString();
//			IndexedWord tmp_dep = depcy.getDependent(); //this sometimes looses the augmented labels, thus hack: get node by index
//			IndexedWord dep = semgraph.getNodeByIndex(tmp_dep.index());
//			//System.out.println(node.index()+ " " + dep.index());
//			if (isGRCT) { //before looking at children, see if we need to output node
//				//right edge
//				if (node.index()<dep.index() && !used.contains(node))  {
//					used.add(node);
//					if (outputformat == Output.POS_OVER_WORD)
//						out.append("("+node.tag() + " " +node.value()+")");
//					else
//						out.append("("+node.value()+")");
//				}
//			}
//			if (!isDWT)
//				out.append("("+reln + " ");
//		  
//			if (isGRCT) {
//				if (semgraph.outDegree(dep)==0)  {
//					used.add(dep);
//					if (outputformat == Output.POS_OVER_WORD)
//						out.append("("+dep.tag() + " " +dep.value()+")");
//					else
//						out.append("("+dep.value()+")");
//				}
//			} else {
//				if (isDWT) {
//					if (outputformat == Output.WORD)
//						out.append("("+dep.value());
//					else if (outputformat == Output.DEP)
//						out.append("("+reln);
//					else
//						out.append("("+dep.value()); //default
//				} else
//					if (outputformat == Output.POS_OVER_WORD)
//				    	out.append("("+dep.tag()+" ("+dep.value()+""); //if includePos: add Pos-tag above word
//				    else
//				    	out.append("("+dep.value());
//			    if (semgraph.outDegree(dep)>0)
//			    	out.append(" ");
//			}
//		
//		  
//		    //add children
//			List<SemanticGraphEdge> edges =  semgraph.getOutEdgesSorted(dep);
//			int i=0;
//			
//		    for (SemanticGraphEdge depChild : edges) {
//		    	int indexNextEdge=-1;
//		    	if (i < (edges.size()-1))
//		    		indexNextEdge = edges.get(i+1).getDependent().index();
//		    	boolean nextEdgeOtherSide = false;
//		    	if (indexNextEdge!= -1 && dep.index()<indexNextEdge)
//		    		nextEdgeOtherSide=true;
//		    	
//		    	IndexedWord child = depChild.getDependent();
//		    	String childRel = depChild.getRelation().toString();
//		    	i++;
//		    	if (isGRCT) {
//		    		//check indices: if child is before head (left edge)
//		    		if (child.index() < dep.index()) {//output child 
//		    			out.append("("+childRel+" ");
//		    			used.add(child);
//		    			if (outputformat == Output.POS_OVER_WORD)
//							out.append("("+child.tag() + " " +child.value()+")");
//						else
//							out.append("("+child.value()+")");
//		    			out.append(")"); //close dep around child
//		    		}
//		    		else if (used.contains(dep) && semgraph.outDegree(child)==0) {
//		    			out.append("("+childRel+" ");
//		    			used.add(child);
//		    			if (outputformat == Output.POS_OVER_WORD)
//							out.append("("+child.tag() + " " +child.value()+")");
//						else
//							out.append("("+child.value()+")");
//		    			out.append(")"); //close dep around child
//		    		}
//		    		else if (!used.contains(child) && !used.contains(dep)) { //output dependent =head
//		    			used.add(dep);
//		    			if (outputformat == Output.POS_OVER_WORD)
//							out.append("("+dep.tag() + " " +dep.value()+")");
//						else
//							out.append("("+dep.value()+")");
//		    		}
//		    		if (!used.contains(dep) && child.index() < dep.index() && nextEdgeOtherSide) {
//		    			used.add(dep);
//		    			if (outputformat == Output.POS_OVER_WORD)
//							out.append("("+dep.tag() + " " +dep.value()+")");
//						else
//							out.append("("+dep.value()+")");
//		    		}
//		    		if  (!used.contains(child) && used.contains(dep) && semgraph.outDegree(child)==0){
//		    			out.append("("+childRel+" ");
//		    			used.add(child);
//		    			if (outputformat == Output.POS_OVER_WORD)
//							out.append("("+child.tag() + " " +child.value()+")");
//						else
//							out.append("("+child.value()+")");
//		    			out.append(")"); //close dep around child
//		    		} 
//		    	}
//		    	else {
//		    		if (!isDWT)
//		    			out.append("("+childRel + " ");
//		    		if (isDWT) {
//			    			if (outputformat == Output.WORD)
//			    				out.append("("+child.value()+"");
//			    			else if (outputformat == Output.DEP)
//			    				out.append("("+childRel);
//			    			else
//			    				out.append("("+child.value()+"");//default
//			    		}
//			    		else 
//			    			if (outputformat == Output.POS_OVER_WORD)
//					    		out.append("("+child.tag()+" ("+child.value()+""); //insert POS above
//					    	else
//					    		out.append("("+child.value());
//		    		//used.add(child);
//		    	}
//		    	
//		    	if (semgraph.outDegree(child)>0){
//		    		out.append(" ");
//		    		traverseGraph(semgraph, child, used,isGRCT,isDWT,outputformat,out);
//		    	} /////
//		    	 	
//		    	
//		    	
//		    	if (!isGRCT) out.append(")");
//		    	if (!isDWT && !isGRCT) out.append(")");
//		    	
//		    	
//		    	if (outputformat == Output.POS_OVER_WORD && isGRCT==false) out.append(")");
//		    } //add Children
//		    out.append(")"); //close add dep.value
//		    if (isGRCT && !used.contains(dep)) {
//				used.add(dep);
//				if (outputformat == Output.POS_OVER_WORD)
//					out.append("("+dep.tag() + " " +dep.value()+")");
//				else
//					out.append("("+dep.value()+")");
//			}
//		     if (!isGRCT) 
//		    	 if (!isDWT) out.append(")"); // close add reln
//		 
//		     if (outputformat == Output.POS_OVER_WORD && isGRCT==false) out.append(")");
//
//		}
//		
//		if (isGRCT && !used.contains(node)) {
//			used.add(node);
//			if (outputformat == Output.POS_OVER_WORD)
//				out.append("("+node.tag() + " " +node.value()+")");
//			else
//				out.append("("+node.value()+")");
//		}
//	}
		  
	
	public void replaceWords(BrownWordCluster wordCluster) {
		for (int i=0; i < this.forms.length; i++) {
			String word = this.forms[i];
			String bitstring = wordCluster.getFullClusterId(word);
			if (bitstring != null)
				this.forms[i] = bitstring;
		}
		
	}
	
	public double getScore() {
		return this.score;
	}

	//Delimiter for additional labels (like targets)
	private static String delim = "@";

	/***
	 * Add entity/mention information to vertex in graph
	 * We add it to the "category" index of CoreLabel in a particular format 
	 * (prefix label prefix2 label), later this info will be used to construct the tree
	 * 
	 * Note: semgraph uses tokens staring from 1 (not 0), this will be added implicitly here
	 * @param tokenIds
	 * @param label
	 * @param semGraph: graph to update
	 * @return updated semGraph
	 */
	public SemanticGraph decorateDependency(int[] tokenIds, String prefix, String label, SemanticGraph semGraph) {
		if (tokenIds.length==1) {
			int index = tokenIds[0]+1; //tokenIds was assumed to be 0-based, but semgraph uses 1-based indices
			try { 
			IndexedWord word = semGraph.getNodeByIndex(index);
			if (word.category()!= null && word.category().length()>0)
				word.setCategory(word.category()+ " " + prefix+" "+label); //append
			else
				word.setCategory(prefix+" "+label);
			
			} catch (IllegalArgumentException exp) {
				System.err.println("part of multi-word-token not found, try next");
				for (int i=0;i<tokenIds.length;i++) {
					index = tokenIds[i]+1;
					if (semGraph.getNodeByIndexSafe(index) != null) {
						semGraph= decorateDependency(new int[]{tokenIds[i]}, prefix, label, semGraph);
						break;
					}
				}
			}
			return semGraph;
			
		} else { //multi-token mention
			try {
				int index = findHighest(tokenIds,semGraph); //find highest parent (head)
				int[] oneToken = new int[] { index };
				return decorateDependency(oneToken, prefix, label,semGraph);
				
			} catch (IllegalArgumentException exp) {
				System.err.println("part of multi-word-token not found, try next");
				for (int i=0;i<tokenIds.length;i++) {
					int index = tokenIds[i]+1;
					if (semGraph.getNodeByIndexSafe(index) != null) {
						semGraph = decorateDependency(new int[]{tokenIds[i]}, prefix, label, semGraph);
						break;
					}
				}
				return semGraph;
			}
		}
	}
	
//	/***
//	 * add mention/entity information (=label) to tokens; 
//	 * Note: semgraph uses tokens staring from 1 (not 0), this will be added implicitly here
//	 * @param tokenIds
//	 * @param label
//	 * @param insert: if true than insert new node above
//	 * @param semGraph: graph to update
//	 * @return updated semGraph
//	 */
//	private SemanticGraph decorateDependency(int[] tokenIds, String label, boolean insert, SemanticGraph semGraph) {
//		if (tokenIds.length==1) {
//			int index = tokenIds[0]+1; //tokenIds was assumed to be 0-based, but semgraph uses 1-based indices
//			try { 
//			IndexedWord word = semGraph.getNodeByIndex(index);
//			IndexedWord parentOfWord = semGraph.getParent(word);
//			GrammaticalRelation grel;
//			SemanticGraphEdge edge;
//			if (parentOfWord==null) { //then it's the root, add that
//				//need to add label to root..
//				
//				edge = incomingEdges.get(0);
//				grel = edge.getRelation();
//				
//				//grel = GrammaticalRelation.valueOf(Language.English, "ROOT");
//				//CoreLabelTokenFactory fac = new CoreLabelTokenFactory();
//				//CoreLabel cl1 = fac.makeToken("ROOT",-1,2);
//				//cl1.setIndex(1001);
//				//IndexedWord newRoot = new IndexedWord(cl1);
//				//edge = new SemanticGraphEdge(newRoot, word, grel, 1.0);
//			} else {
//				edge = semGraph.getEdge(parentOfWord, word);
//				grel = edge.getRelation();
//			}
//			if (!insert) {
//				//update dependency relation, append label!
//				GrammaticalRelation reln = GrammaticalRelation.valueOf(Language.English, grel.getShortName()+delim+label);
//				edge.setRelation(reln);
//				return semGraph;
//			} else {
//				//insert node between parent and word with label 
//				//insertNodeBetween(word,parentOfWord,label);
//				System.err.println("InsertNode not yet implemented");
//				System.exit(-1);
//				return null;
//			}
//			} catch (IllegalArgumentException exp) {
//				System.err.println("part of multi-word-token not found, try next");
//				for (int i=0;i<tokenIds.length;i++) {
//					index = tokenIds[i]+1;
//					if (semGraph.getNodeByIndexSafe(index) != null) {
//						semGraph= decorateDependency(new int[]{tokenIds[i]}, label,insert, semGraph);
//						break;
//					}
//				}
//				return semGraph;
//			}
//		} else { //multi-token mention
//			try {
//				int index = findHighest(tokenIds,semGraph); //find highest parent (head)
//				int[] oneToken = new int[] { index };
//				return decorateDependency(oneToken, label,insert,semGraph);
//				
//			} catch (IllegalArgumentException exp) {
//				System.err.println("part of multi-word-token not found, try next");
//				for (int i=0;i<tokenIds.length;i++) {
//					int index = tokenIds[i]+1;
//					if (semGraph.getNodeByIndexSafe(index) != null) {
//						semGraph = decorateDependency(new int[]{tokenIds[i]}, label,insert, semGraph);
//						break;
//					}
//				}
//				return semGraph;
//			}
//		}
//	}
	

//	/** 
//	 * Decorate token with given ids with label 
//	 * if pos is true: decorate pos rather than word itself
//	 * @param tokenIds1
//	 * @param label
//	 */
//	public void decorateTokens(int[] tokenIds, String label) {
//		if (this.semGraphAltered==null) {
//			this.semGraphAltered =  SemanticGraphFactory.makeFromTree(this.grammaticalStructure, "",0); //dummy docId and sentenceId;
//			decorateTokensHelper(tokenIds, label,false, this.semGraphAltered);
//		} else {
//			decorateTokensHelper(tokenIds, label,false, this.semGraphAltered);
//		}
//	}
//	
//	public void decorateTokens(int[] tokenIds, String label, boolean pos) {
//		if (this.semGraphAltered==null) {
//			this.semGraphAltered =  SemanticGraphFactory.makeFromTree(this.grammaticalStructure, "",0); //dummy docId and sentenceId;
//			decorateTokensHelper(tokenIds, label,pos, this.semGraphAltered);
//		} else {
//			decorateTokensHelper(tokenIds, label,pos, this.semGraphAltered);
//		}
//	}
//	
//	//alters semGraphAltered

//	private void decorateTokensHelper(int[] tokenIds, String label ,boolean pos, SemanticGraph semGraph) {
//		if (tokenIds.length==1) {
//			int index = tokenIds[0]+1; //tokenIds was assumed to be 0-based, but semgraph uses 1-based indices
//			try {
//		    IndexedWord word = semGraph.getNodeByIndex(index);
//			if (!pos) {
//				//update word
//				//word.setWord(word.value() + delim +label);
//				word.setValue(word.value() + delim +label);
//			} else {
//				//update pos
//				word.setTag(word.tag() + delim + label);
//			}
//			//check if it worked
//			//IndexedWord w = semGraph.getNodeByIndex(index);
//			//System.out.println(w.value());
//			} catch (IllegalArgumentException exp) {
//				System.err.println(exp);
//			}
//		} else {
//			
//			try {
//				int index = findDeepest(tokenIds, semGraph)+1;		
//				IndexedWord word = semGraph.getNodeByIndex(index);
//				
//				if (!pos) {
//					//update word
//					word.setWord(word.value() + delim +label);
//					word.setValue(word.value() + delim +label);
//				} else {
//					//update pos
//					word.setTag(word.tag() + delim + label);
//				}
//			} catch (IllegalArgumentException e) {
//				System.err.println("part of multi-word-token not found, try next");
//				for (int i=0;i<tokenIds.length;i++) {
//					int index = tokenIds[i]+1;
//					if (semGraph.getNodeByIndexSafe(index) != null) {
//						decorateTokensHelper(new int[]{tokenIds[i]}, label,pos, this.semGraphAltered);
//						break;
//					}
//				}
//			}
//			
//			
//		}
//	}

	/***
	 * Check if two tokens (represented by tokenIds integer array) overlap
	 * @param tokenIds1
	 * @param tokenIds2
	 * @return true if some tokens are the same
	 */
	public static boolean overlapping(int[] tokenIds1, int[] tokenIds2) {
		for (int i=0; i<tokenIds1.length;i++) {
			for (int j=0; j<tokenIds2.length; j++) {
				if (tokenIds1[i]==tokenIds2[j])
					return true;
			}
				
		}
		return false;
	}
	
	/***
	 * Initialize semantic graph from grammatical structure
	 * @return
	 */
	public SemanticGraph initSemanticGraph() {
		return SemanticGraphFactory.makeFromTree(this.grammaticalStructure, "",0);   //dummy docId and sentenceId;
	}
	
	/***
	 * Get SemanticGraph that constitutes path between two tokens
	 * @param tokenIds1
	 * @param tokenIds2
	 * @return SemanticGraph or null if an error occurred
	 */
	public SemanticGraph getPath(int[] tokenIds1, int[] tokenIds2, SemanticGraph graph) {
		try { 
			//if mentions are just 1 token long
			if (tokenIds1.length == 1 && tokenIds2.length == 1) {
				int index1 = tokenIds1[0]+1; //tokenIds was assumed to be 0-based, but semgraph uses 1-based indices
				IndexedWord word1 = graph.getNodeByIndex(index1);
				int index2 = tokenIds2[0]+1;
				IndexedWord word2 = graph.getNodeByIndex(index2);
				
				List<SemanticGraphEdge> path = graph.getShortestUndirectedPathEdges(word1,word2);
				if (path == null) {
					System.err.println("Problem creating path between nodes.");
				}
				//return updated semgraph
				return SemanticGraphFactory.makeFromEdges(path);
			}
			else {
				// one of both of the mentions are more than 1 token long
				int index1, index2;
				if (tokenIds1.length>1)
					index1 = findDeepest(tokenIds1, graph)+1; //find the lowest dependent
				else
					index1 = tokenIds1[0]+1; // add one as semgraph tokens start with 1
				if (tokenIds2.length>1)
					index2 = findDeepest(tokenIds2, graph)+1;
				else
					index2 = tokenIds2[0]+1;
				if (index1 == index2) {
					System.err.println("mentions overlap and same head was chosen:" + index1);
					System.exit(-1); // this message should not be thrown!
					
				}
				IndexedWord word1 = graph.getNodeByIndex(index1);
				IndexedWord word2 = graph.getNodeByIndex(index2);
				List<SemanticGraphEdge> path = graph.getShortestUndirectedPathEdges(word1,word2);
				// return updated semgraph
				return SemanticGraphFactory.makeFromEdges(path);
			}
		} catch (IllegalArgumentException exp) {
			System.err.println(exp);
			return null;
		}
	}


	//helper method for multiple-token mentions: find index of token that is the 'deepest'
	//note: returns tokenIds index (= -1 of IndexedWord index)
	private int findDeepest(int[] tokenIds, SemanticGraph semGraph) {
		int first = tokenIds[0]+1;
		IndexedWord deepest = semGraph.getNodeByIndex(first);
		for (int i=1; i < tokenIds.length;i++) {
			IndexedWord parent = semGraph.getNodeByIndexSafe(tokenIds[i]+1);
			if (parent == null)
				continue; // if parent is not there (like in case of collapsed deps...)
			if (semGraph.isAncestor(deepest, parent)!=1)
				if (semGraph.isAncestor(parent, deepest)==1) {
					deepest = parent;
			     }
			else {
				//throw new IllegalArgumentException("problematic: no relation between tokens!");
				System.err.println("Problematic: no relation between head tokens. Backup to default: take first (rest of np might be disregarded)");
				return tokenIds[0];
			}
		}
		return deepest.index()-1;
	}
	
	/***
	 * Method for multiple-token mentions: find index of token that is the 'highest' (head of all)
	 * @param tokenIds
	 * @param semGraph
	 * @return tokenIndex
	 */
	private int findHighest(int[] tokenIds, SemanticGraph semGraph) {
		int first = tokenIds[0]+1;
		IndexedWord highest = semGraph.getNodeByIndex(first);
		for (int i=1; i < tokenIds.length;i++) {
			IndexedWord child = semGraph.getNodeByIndex(tokenIds[i]+1);
			if (semGraph.isAncestor(child, highest)!=1)
				if (semGraph.isAncestor(highest, child)==1) {
					highest = child;
			     }
			else {
				//throw new IllegalArgumentException("problematic: no relation between tokens!");
				System.err.println("Problematic: no relation between head tokens. Backup to default: take first (rest of np might be disregarded).");
				return tokenIds[0]; //in head's annotation
			}
		}
		return highest.index()-1;
	}

	public void prettyPrintSemGraph() {
		semGraph.prettyPrint();	
	}

	// find the longer array and get the ids that are not in the shorter
	public static int[] chooseOther(int[] tokenIds1, int[] tokenIds2) {
		ArrayList<Integer> newIds1 = new ArrayList<Integer>();
		ArrayList<Integer> newIds2 = new ArrayList<Integer>();
		for (int i=0;i<tokenIds1.length;i++)
			newIds1.add(tokenIds1[i]);
		for (int i=0;i<tokenIds2.length;i++)
			newIds2.add(tokenIds2[i]);
		if (tokenIds1.length>tokenIds2.length) {
			newIds1.removeAll(newIds2);
			int[] newi = new int[newIds1.size()];
			for (int i=0; i<newIds1.size();i++)
				newi[i]=newIds1.get(i);
			return newi;
		} else {
			newIds2.removeAll(newIds1);
			int[] newi = new int[newIds2.size()];
			for (int i=0; i<newIds2.size();i++)
				newi[i]=newIds2.get(i);
			return newi;
		}
	}

	/***
	 * Get surface string from dependency instance
	 * @return
	 */
	public String getSentenceSurface() {
	    StringBuilder sb = new StringBuilder();
	    for (int i=1; i < this.forms.length; i++) //start from 1 as 0 is <root>
	    	sb.append(forms[i] + " ");
		return sb.toString().trim();
	}

	/***
	 * Print to CoNLL format
	 */
	public void printConll() {
		Tree t = grammaticalStructure.root();
		boolean conllx = true;
		boolean extraSep = false;
		GrammaticalStructure.printDependencies(grammaticalStructure, grammaticalStructure.typedDependencies(false), t, conllx, extraSep);
		
	}
	
	
	
	/***
	 * Checks whether parentheses match
	 * @param bracketed tree
	 * @return true/false
	 */
	public static boolean checkParentheses(String s) {
	    int nesting = 0;
	    for (int i = 0; i < s.length(); ++i)
	    {
	        char c = s.charAt(i);
	        switch (c) {
	            case '(':
	                nesting++;
	                break;
	            case ')':
	                nesting--;
	                if (nesting < 0) {
	                    return false;
	                }
	                break;
	        }
	    }
	    return nesting == 0;
	}

	/****
	 * Mark nodes that are on the path between the two nodes
	 * @param semGraph2
	 * @param graphPath
	 */
	public void markNodesOnPath(SemanticGraph graph, SemanticGraph graphPath) {
		for (int index=1; index<= this.forms.length;index++) {
			// check for every node if it exists in graphPath, if so, add annotation using decorateDependency
			IndexedWord node = graphPath.getNodeByIndexSafe(index);
			if (node != null) {
				// node is on path
				String prefix = "pathNode";
				String label = "P";
				int[] tokenIds = new int[]{index-1}; //tokenIds start at 0
				decorateDependency(tokenIds, prefix, label, graph);
			}
		}
	}
	
	/****
	 * Mark edges that are on the path between the two nodes
	 * @param semGraph2
	 * @param graphPath
	 */
	public void markEdgesOnPath(SemanticGraph graph, SemanticGraph graphPath) {
		
		for (int index=1; index<= this.forms.length;index++) {
			// check for every node if it exists in graphPath, if so, add annotation using decorateDependency
			IndexedWord node = graphPath.getNodeByIndexSafe(index);
			if (node != null) {
				// node is on path
				IndexedWord parentOfWord = graph.getParent(node);
				SemanticGraphEdge edge = graph.getEdge(parentOfWord, node);
				GrammaticalRelation grel = edge.getRelation();
				
				String prefix = "pathDep";
				String label = grel.toString()+"P";
				int[] tokenIds = new int[]{index-1}; //tokenIds start at 0
				decorateDependency(tokenIds, prefix, label, graph);
			}
		}
	}

	

}
