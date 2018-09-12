package limo.core.interfaces;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import limo.core.Entities;
import limo.core.Relations;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.core.trees.TreeManager;
import limo.core.trees.constituency.ParseTree;
import limo.core.trees.dependency.DependencyInstance;
import limo.core.trees.dependency.io.DependencyReader;
/**
 * IRelation document
 * 
 * @author Barbara Plank
 *
 */
public abstract class IRelationDocument {
	
	
	protected ArrayList<Sentence> sentences;
	protected TreeManager treeManager = new TreeManager();
	
	public abstract Entities getEntities();
	
	public abstract Relations getRelations();
	
	public abstract ArrayList<Sentence> getSentences();
	
	public abstract int getNumSentences();

	public abstract String getURI();
	
	public abstract void saveTokenizedTextAsFile(File outDir);

	public abstract Sentence getSentenceById(int currentIndex);

	public abstract int getNumMentions();

	public abstract int getNumEntities();
	
	public abstract int getNumRelations();

	public AbstractTree getBestConstituentParseTreeBySentenceId(int id) throws Exception {
		if (this.treeManager.getNumConstituentTrees()!=0)
			return (ParseTree) this.treeManager.getBestConstituentTreeForSentence(id);
		else
			throw new Exception("TreeManager contains no trees!");
	}
	
	public ParseTree getBestConstituentParseTreeSecondParserBySentenceId(
			int sentenceId) throws Exception {
		if (this.treeManager.getNumConstituentTreesSecondParser()!=0)
			return (ParseTree) this.treeManager.getBestConstituentTreeSecondParserForSentence(sentenceId);
		else
			throw new Exception("TreeManager contains no trees of second parser!");
	}

	
	public DependencyInstance getBestDependencyTreeBySentenceId(int sentenceId) throws Exception {
		if (this.treeManager.getNumDependencyTrees()!=0)
			return (DependencyInstance) this.treeManager.getBestDependencyInstancesForSentence(sentenceId);
		else
			throw new Exception("TreeManager contains no dependency trees!");
	}

	public void addConstituencyData(String absolutePath) {
		
		ArrayList<ArrayList<ParseTree>> constituentParseTrees = new ArrayList<ArrayList<ParseTree>>();

		// read parsed data
		try {
			BufferedReader inputReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(absolutePath), "UTF-8"));
			String line = inputReader.readLine();
			StringBuilder parseTreeStr = new StringBuilder();
			int currentIndex = 0;
			while (line != null) {
				if (!line.equals(""))
					parseTreeStr.append(line);
				else {

					ParseTree tree = new ParseTree(parseTreeStr
							.toString());
					parseTreeStr = new StringBuilder();

					// System.out.println(tree.getTerminalsAsString());
					ArrayList<ParseTree> parses =new ArrayList<ParseTree>();
					parses.add(tree);
					
					constituentParseTrees.add(parses);

					// check if terminals match
					Sentence s = this.getSentenceById(currentIndex);

					String terminalsTree = tree
							.getTerminalsAsString();
					String sentence = s.toString();
					if (!terminalsTree.equals(sentence)) {
						System.err
								.println("--***** ERROR Terminals differ! "
										+ this.getURI()
										+ " sentence:"
										+ currentIndex);
						System.err.println(terminalsTree);
						System.err.println(sentence);
					}

					currentIndex += 1;
				}
				line = inputReader.readLine();
				// System.out.println(line);
			}
			inputReader.close();
			this.treeManager.setConstituentParseTrees(constituentParseTrees);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addDependencyData(String absolutePath, boolean nbestMode)  {
		try {
			ArrayList<ArrayList<DependencyInstance>> dependencyTrees = new ArrayList<ArrayList<DependencyInstance>>();

			DependencyReader depReader;

			depReader = DependencyReader.createDependencyReader("CONLL07",
					nbestMode);

			depReader.startReading(absolutePath);

			ArrayList<DependencyInstance> parsesOfSentence;

			while ((parsesOfSentence = depReader.getNext()) != null) {
				dependencyTrees.add(parsesOfSentence);
			}

			this.treeManager.setDependencyTrees(dependencyTrees);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public int getNumParseTrees() {
		return this.treeManager.getNumConstituentTrees();
	}

	public int getNumDependencyTrees() {
		return this.treeManager.getNumDependencyTrees();
	}
	
	//if second parser is used
	public int getNumParseTreesSecondParser() {
		return this.treeManager.getNumConstituentTreesSecondParser();
	}

}
