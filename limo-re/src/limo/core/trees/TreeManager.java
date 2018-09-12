package limo.core.trees;

import java.util.ArrayList;

import limo.core.trees.constituency.ParseTree;
import limo.core.trees.dependency.DependencyInstance;

/***
 * Class to handle parse trees for an IRelationDocument
 * 
 * @author Barbara Plank
 *
 */
public class TreeManager {
	
	
	private ArrayList<ArrayList<ParseTree>> constituentParseTrees;
	private ArrayList<ArrayList<ParseTree>> constituentParseTreesParser2;
	
	ArrayList<ArrayList<DependencyInstance>> dependencyParseTrees;
	
	public TreeManager() {
		constituentParseTrees = new ArrayList<ArrayList<ParseTree>>();
		dependencyParseTrees = new ArrayList<ArrayList<DependencyInstance>>();
	}

	/**
	 * Add a ParseTree to the list of trees for sentence with sentenceIndex
	 * @param sentenceIndex
	 * @param tree
	 */
	public void addConstituentTree(int sentenceIndex, ParseTree tree) {
		ArrayList<ParseTree> treeList;
		if (constituentParseTrees.size() == 0 || sentenceIndex>=constituentParseTrees.size()) {
			treeList = new ArrayList<ParseTree>();
			treeList.add(tree);
			constituentParseTrees.add(sentenceIndex, treeList);
		}
		else {
			treeList = constituentParseTrees.get(sentenceIndex);
			treeList.add(tree);
		}
	}
	
	public void addDependencyTree(int sentenceIndex, DependencyInstance depTree) {
		ArrayList<DependencyInstance> treeList;
		if (dependencyParseTrees.get(sentenceIndex)== null) {
			treeList = new ArrayList<DependencyInstance>();
			treeList.add(depTree);
			dependencyParseTrees.add(sentenceIndex, treeList);
		}
		else {
			treeList = dependencyParseTrees.get(sentenceIndex);
			treeList.add(depTree);
		}
	}

	public ParseTree getBestConstituentTreeForSentence(int id) {
		return constituentParseTrees.get(id).get(0);
	}
	
	public DependencyInstance getBestDependencyInstancesForSentence(int id) {
		return dependencyParseTrees.get(id).get(0);
	}
	
	public ParseTree getBestConstituentTreeSecondParserForSentence(int sentenceId) {
		return constituentParseTreesParser2.get(sentenceId).get(0);
	}

	public int getNumConstituentTrees() {
		return this.constituentParseTrees.size();
	}
	
	public int getNumDependencyTrees() {
		return this.dependencyParseTrees.size();
	}

	/***
	 * Sets the current list of parse trees
	 * If that's already given, assumes that the data comes from a seond parser
	 * TODO: generalize to n parsers
	 * @param constituentParseTrees
	 */
	public void setConstituentParseTrees(
			ArrayList<ArrayList<ParseTree>> constituentParseTrees) {
		if (this.constituentParseTrees.size() == 0)
			this.constituentParseTrees = constituentParseTrees;
		else {
			//assume data from second parser
			this.constituentParseTreesParser2 =  new ArrayList<ArrayList<ParseTree>>();
			this.constituentParseTreesParser2 = constituentParseTrees;
		}
	}
	
	public void setDependencyTrees(ArrayList<ArrayList<DependencyInstance>> dependencyTrees) throws Exception {
		if (this.dependencyParseTrees.size() == 0) {
			this.dependencyParseTrees = dependencyTrees;
		} 
		else
			throw new Exception("deptrees not initialized");
	}

	public int getNumConstituentTreesSecondParser() {
		return this.constituentParseTreesParser2.size();
	}

	
}
