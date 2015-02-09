/* Modified by Barbara Plank
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
package limo.exrel.features;


import java.util.ArrayList;

import limo.cluster.BrownWordCluster;
import limo.cluster.ClassificationScores;
import limo.cluster.SemKernelDictionary;
import limo.cluster.Word2VecSemKernelDictionary;
import limo.cluster.WordEmbeddingDictionary;
import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.exrel.features.re.structured.RelationExtractionStructuredFeature;

public class RelationExtractionStructuredFeaturesGroup extends FeaturesGroup<RelationExtractionStructuredFeature> {
	
	public RelationExtractionStructuredFeaturesGroup(String id) {
		super(id);
	}

	@Override
	public String extract(Object... args) throws Exception {
		AbstractTree parseTree = (AbstractTree)args[0];
		Mention mention1 = (Mention)args[1];
		Mention mention2 = (Mention)args[2];
		Relation relation = (Relation)args[3];
		Sentence sentence = (Sentence)args[4];
		//optional
		ArrayList<Object> resources = new ArrayList<Object>();
		BrownWordCluster wordCluster = null;
		ClassificationScores scores = null;
		SemKernelDictionary semkernelDict = null;
		//thien's addition
		Word2VecSemKernelDictionary word2vecSemKernelDict = null;
		WordEmbeddingDictionary wordEmbeddingDict = null;
		//end thien's addition
		if (args[5]!= null) 
			wordCluster = (BrownWordCluster)args[5];
		if (args.length >=7 && args[6]!= null)
			scores = (ClassificationScores)args[6];
		if (args.length >=8 && args[7]!= null) 
			semkernelDict = (SemKernelDictionary)args[7];
		//thien's addition
		if (args.length >= 9 && args[8]!= null)
			word2vecSemKernelDict = (Word2VecSemKernelDictionary) args[8];
		if (args.length >= 10 && args[9]!= null)
			wordEmbeddingDict = (WordEmbeddingDictionary) args[9];
		//end thien's addition
		resources.add(wordCluster);
		resources.add(scores);
		resources.add(semkernelDict);
		resources.add(word2vecSemKernelDict);
		resources.add(wordEmbeddingDict);
		StringBuffer result = new StringBuffer();
		for (RelationExtractionStructuredFeature feature : features) {
			if (result.length() > 0) {
				result.append(" ");				
			}
			
			result.append(feature.extract(parseTree, mention1, mention2, relation, sentence, resources));
		}
		return result.toString();
	}
	
}