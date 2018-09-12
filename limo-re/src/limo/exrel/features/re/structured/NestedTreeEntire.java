package limo.exrel.features.re.structured;

import java.util.ArrayList;

import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import limo.core.Sentence;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.trees.AbstractTree;
import limo.core.trees.dependency.DependencyInstance;

/***
 * Nested tree for dependency structures   DO NOT USE
 * 
 * @author Barbara Plank
 *
 */
public class NestedTreeEntire extends RelationExtractionStructuredFeature {

	@Override
	protected String _extract(AbstractTree abstractTree, Mention mention1,
			Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) {
		
		
		int[] tokenIds1 = mention1.getTokenIds();
		int[] tokenIds2 = mention2.getTokenIds();
		
		DependencyInstance depTree = (DependencyInstance)abstractTree;

		SemanticGraph semGraph = depTree.getSemanticGraph();
				
		String firstTarget = "E1-"+mention1.getType()+"-"+mention1.getEntityReference().getType();
		String secondTarget = "E2-"+mention2.getType()+"-"+mention2.getEntityReference().getType();
		try {
			if (DependencyInstance.overlapping(tokenIds1,tokenIds2)) {
				if (tokenIds1.length>tokenIds2.length)
					tokenIds1 = DependencyInstance.chooseOther(tokenIds1,tokenIds2);
				else
					tokenIds2 = DependencyInstance.chooseOther(tokenIds1,tokenIds2);
			}
			

			//SemanticGraph semGraph = depTree.getPath(tokenIds1, tokenIds2, depTree.getSemanticGraph());
			//SemanticGraph semGraph = depTree.getSemanticGraph();
			semGraph = depTree.decorateDependency(tokenIds1, "target", firstTarget, semGraph);
			semGraph = depTree.decorateDependency(tokenIds2, "target", secondTarget, semGraph);
			
		
			// decorate all other mentions in sentence
			for (Mention mention : sentence.getMentions()) {
				if (!mention.equals(mention1) && !mention.equals(mention2) ) { 
					//decorate mention
					//String label = "E-"+mention.getType()+"-"+mention.getEntityReference().getType();
				      			
					int[] tokenIds = mention.getTokenIds();
					semGraph = depTree.decorateDependency(tokenIds, "non-target", "E", semGraph);
				}
			}
			
			String path = depTree.getNestedTree(semGraph);		
			
			if (!(path.contains(firstTarget) || path.contains(secondTarget))) {
	 			System.err.println("Ignoring problematic tree: "+ path);
	 			System.err.println(mention1.getHead());
	 			System.err.println(mention2.getHead());
				return "";
			}
			if (DependencyInstance.checkParentheses(path) ==false) {
				System.err.println("Parenthesis do not match!");
				System.err.println(path);
				System.exit(-1);
			}
			return path;
		} catch (NullPointerException e) {
			System.err.println("Problem with instance: "+depTree.toString());
			System.err.println("and mentions: "+ mention1 + " "+ mention2);
			System.err.println(e.getMessage());;
			return "";
		}
		
		
	}

	
	
}
