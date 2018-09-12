/*
 * Updated and modified by Barbara Plank, 2012.
 * 
 * -----
 * 
 * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
 * 
 * This software is released under a double licensing scheme.
 * 
 * For personal or research uses, the software is available under the
 * GNU Lesser GPL (LGPL) v.3 license. 
 * 
 * See the file LICENSE in the source distribution for more details.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package limo.core.trees.constituency;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import limo.core.trees.AbstractTree;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.trees.PennTreeReaderFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;


public class ParseTree extends AbstractTree {
	
	private Tree tree;
	
	// dummy empty constructor
	private ParseTree() {}

	public ParseTree(String surface) throws IOException {
		this();
		this.tree = createTree(surface);
		this.tree.indexSpans(0);
		this.tree.indexLeaves();
	}
	
	private static Tree createTree(String surface) {
		try {
			return Tree.valueOf(surface, new PennTreeReaderFactory());
		}
		catch (Exception ioe) {
		      throw new RuntimeException("Tree.valueOf() tree construction failed", ioe);
		}
	}

	public boolean equals(Object obj) {
		return ((ParseTree)obj).toString().equals(toString());
	}

	public Tree getRootNode() {
		return this.tree;
	}

	public List<Tree> getTerminals() {
		return this.tree.getLeaves();
	}
	

	public Tree getTerminal(int terminal) {
		return this.tree.getLeaves().get(terminal);
	}
	
	public String getTerminalSurface(int i) {
		Tree tree =  this.tree.getLeaves().get(i);
		return tree.label().value();
	}


	public int getNodeId(Tree node) {
		return node.nodeNumber(getRootNode());
	}

	public void hide(Tree node) {
		try {
			removeNode(node);
		} catch (IndexOutOfBoundsException e) {
			return;
		}
	}
	
	private void removeNode(Tree node) {
		Tree parent;
		if (node.isLeaf()) {
			parent = tree.getNodeNumber(getNodeId(node)).parent(tree).parent(tree);
			node = tree.getNodeNumber(getNodeId(node)).parent(tree);
		}
		else
			parent = tree.getNodeNumber(getNodeId(node)).parent(tree);
			//parent =  node.parent(tree);
		int i=0;
		for (Tree kid : parent.children()) {
			if (kid.equals(node)) {
					parent.removeChild(i);
				}
			i++;
		}
	}


	
	public String toString() {
		return toStringBuilder(new StringBuilder(), this.tree).toString();
	}

	private StringBuilder toStringBuilder(StringBuilder sb, Tree node) {
		if (node.isLeaf()) {
			if (node.label() != null) {
				sb.append(node.label().value());
			}
			return sb;
		} else {
			if (node.getHide()==true) {
				if (sb.length()>=1)
					return sb.deleteCharAt(sb.length()-1);
				else
					return sb;
				
			}
			
			sb.append('(');
			if (node.label() != null) {
				sb.append(node.label().value());
			}

		}
		Tree[] kids = node.children();
		if (kids != null) {
			for (Tree kid : kids) {
				sb.append(' ');
				toStringBuilder(sb,kid);
			}
		}		
		return sb.append(')');
	}
	
	/*** get minimum complete tree 
	 * @throws IOException ***/
	public String MCT(int start, int end) throws IOException {
		if (start>end) { //invert
			int tmp = start;
			start = end;
			end = tmp;
		} 
		List<Tree> terminalsToKeep = new ArrayList<Tree>();
		int i=0;
		for (Tree terminal : this.getTerminals()) {
			if (i>=start && i <=end)
				terminalsToKeep.add(terminal);
			i++;
		}
		ParseTree output = new ParseTree(this.tree.toString());
		//find smallest common subtree
		Tree smallest = findSmallestCommonSubtree(output,terminalsToKeep);
		return smallest.toString();
	}
	
	
	private Tree findSmallestCommonSubtree(ParseTree output, List<Tree> terminalsToKeep) {
			Iterator<Tree> iter = output.tree.iterator();
			Tree smallest = null;
			while (iter.hasNext()) {
				
				Tree t = iter.next();
			
				boolean dominates=false;
				for (Tree keep : terminalsToKeep) {
					if (anyAncestorOf(t,keep)) {
						dominates=true;
					} else {
						dominates=false; 
						break;
					}
				}
				if (dominates) {
					if (smallest == null) 
						smallest = t;
					else {
						if (smallest.size()>t.size() && !t.isLeaf())
							smallest = t;
					}
				}
					
			}
			return smallest;
	}

	/** Path-enclosed tree 
	 * @throws IOException **/
	public String PET(int start, int end) throws IOException {
		if (start>end) { //invert
			int tmp = start;
			start = end;
			end = tmp;
		} 
		
		String mct_str = MCT(start,end);
		
		ParseTree mct = new ParseTree(mct_str);
		
		//new index!
		List<Tree> terminalsCovered = this.getCoveredTerminals(start,end);

		//get offset
		String orgSent = this.getTerminalsAsString();
		String mctSent = mct.getTerminalsAsString();
		
		String prefixSent = orgSent.substring(0, orgSent.indexOf(mctSent));
		int offset =0;
		if (prefixSent.length()>0 || (prefixSent.length()==0 && start>0 && start>mct.getTerminalsAsString().length())) {
			offset = prefixSent.split(" ").length;
			//if mctSent is before start, have a problem as it appears several times, get next
			int lengthMCT = mctSent.split(" ").length;
			while (offset<(start-lengthMCT)) {
				
				prefixSent = orgSent.substring(0, orgSent.indexOf(mctSent,prefixSent.length()+1));
				offset = prefixSent.split(" ").length;
				lengthMCT = mctSent.split(" ").length;
			}
				
			
		}

		List<Integer> terminalsToRemoveBefore = new ArrayList<Integer>();
		List<Integer> terminalsToRemoveAfter = new ArrayList<Integer>();
		List<Integer> terminalsToKeep = new ArrayList<Integer>();
		int i=0;
		int id=0;
		for (Tree terminal : mct.getTerminals()) {
			if (i < terminalsCovered.size() && equalSpan(terminalsCovered.get(i),terminal,offset)) {
				terminalsToKeep.add(id);
				i++;
			}	
			else
				if (terminalsToKeep.size() == 0 )
					terminalsToRemoveBefore.add(id);
				else
					terminalsToRemoveAfter.add(id);
			
			id++;
		}
		Iterator<Tree> iter = mct.getIterator();
		while (iter.hasNext()) {
			Tree c = iter.next();
			if (c.isLeaf())
				continue;
			
			CoreLabel l = (CoreLabel) c.label();
			int beginSpan = l.get(BeginIndexAnnotation.class);
			int endSpan = l.get(EndIndexAnnotation.class)-1;
	
			if (terminalsToRemoveBefore.contains(beginSpan) && terminalsToRemoveBefore.contains(endSpan)) {
				
				c.setHide(true);
			}
			else if (terminalsToRemoveAfter.contains(beginSpan) && terminalsToRemoveAfter.contains(endSpan)) {
				c.setHide(true);
			}
			
		}		
		return mct.toString();
	}

	
	private int getStartIndex(Tree tree) {
		CoreLabel l1 = (CoreLabel) tree.label();
		return l1.get(BeginIndexAnnotation.class);
	}
	private int getEndIndex(Tree tree) {
		CoreLabel l1 = (CoreLabel) tree.label();
		return l1.get(EndIndexAnnotation.class);
	}


	// check if two trees span the same terminals = offset to add to tree2
	private boolean equalSpan(Tree tree1, Tree tree2, int offset) {
		int beginSpan1 = getStartIndex(tree1);
		int endSpan1 = getEndIndex(tree1)-1;
		int beginSpan2 = getStartIndex(tree2) + offset;
		int endSpan2 = getEndIndex(tree2)-1+offset;
		if (beginSpan1 == beginSpan2 && endSpan1 == endSpan2)
			return true;
		else
			return false;
	}
	
	// check if two trees span the same terminals = offset to add to tree2
	private boolean containsBySpan(Tree tree1, Tree tree2) {
			int beginSpan1 = getStartIndex(tree1);
			int endSpan1 = getEndIndex(tree1)-1;
			int beginSpan2 = getStartIndex(tree2);
			int endSpan2 = getEndIndex(tree2)-1;
			if (beginSpan1 <= beginSpan2 && endSpan1 >= endSpan2)
				return true;
			else
				return false;
	}

	private List<Tree> getCoveredTerminals(int start, int end) {
		ArrayList<Tree> terminals = new ArrayList<Tree>();
		for (int i=0; i < this.getTerminals().size(); i++) {
			if (i>=start && i <=end)
				terminals.add(this.getTerminals().get(i));
		}
		return terminals;
	}

	public boolean anyAncestorOf(Tree node, Tree t) {		
		Tree parent = t.parent(getRootNode());
		if (!containsBySpan(node,t))
			return false;
		if (node.isPreTerminal() && parent==null) {
			return node.toString().contains(t.toString());
		}
		if (node.equals(getRootNode()))
			return true;
		if (parent ==null)
			return false;
		if (parent != null && node.equals(parent))
			return true;
		if (node.equals(t))
			return true; //node itself
		return anyAncestorOf(node,parent);
	}
	
	
		
	//rather than finding parent, insert nodes above terminals
	public String insertNodes(int[] tokenIds1, String firstTarget,
			int[] tokenIds2, String secondTarget) {
		
		ParseTree parse = transformTree(tokenIds1,firstTarget,this);
		parse = transformTree(tokenIds2,secondTarget,parse);
		return parse.toString();
	}
	
	private ParseTree transformTree(int[] tokenIds, String entityLabel, ParseTree parseTree) {
		try {
		int entityLength = tokenIds.length;
	
		Tree tree = Tree.valueOf(parseTree.toString(), new PennTreeReaderFactory());	
		List<Tree> leaves = tree.getLeaves();
	
		int firstToken = tokenIds[0];
		Tree terminal = leaves.get(firstToken);
		Tree preTerminal = terminal.parent(tree);
		Tree parent = preTerminal.parent(tree); //parent = POS, parent.parent = first Non-terminal
		
		List<Tree> childrenAsList = parent.getChildrenAsList(); //(NNP X)(NNP Y)
		
		
		int childIndex = parent.objectIndexOf(preTerminal);
			    	
		TreeFactory tf = tree.treeFactory();
	    LabelFactory lf = tree.label().labelFactory();
	    
	    //create the new node, add node as child
	    int maxSize = Math.min(childrenAsList.size(), entityLength);
	    int start = (childIndex == 0) ? 0 : childIndex;
	    int end = maxSize + start;
	    Tree left = tf.newTreeNode(lf.newLabel(entityLabel), null);
	    for (int i = start; i < end; i++) {
	    	if (i >= childrenAsList.size())
	    		continue; //ignore those above
	    	left.addChild(childrenAsList.get(i));
	    }
	    
	 // remove all the two first children of t before
	    for (int i = start; i < end; i++) {
	    	if (i >= childrenAsList.size())
	    		continue; //ignore those above
	        parent.removeChild(start);
	    }

	    // add XS as the first child
	    parent.addChild(start, left);
	    
	    return new ParseTree(tree.toString());
		} catch (Exception e) {
			System.err.println("Problem with tree: "+this.toString() + " tokenNum: "+tokenIds[0]);
			return null;
		}
		
	}
	
	//for single node
	public String insertNode(int[] tokenIds1, String target) {
		return transformTree(tokenIds1,target,this).toString();
	}
	

	
	
	/**
	 * GetPathEnclosedTree PET (subset of MCT)
	 * @param tokenIndex1
	 * @param tokenIndex2
	 * @return
	 * @throws IOException 
	 */
	public String getPathEnclosedTree(int tokenIndex1, int tokenIndex2) throws IOException {
		return PET(tokenIndex1, tokenIndex2);		
	}


	public String getTerminalsAsString() {
		List<Tree> ts = this.getTerminals();
		StringBuilder out = new StringBuilder();
		for (Tree n : ts) {
			out.append(n.label().value() + " ");
		}
		return out.toString().trim();
	}

	public Tree getNode(int i) {
		return this.tree.getNodeNumber(i);
	}

	public Iterator<Tree> getIterator() {
		return this.tree.iterator();
	}

	


	
}
