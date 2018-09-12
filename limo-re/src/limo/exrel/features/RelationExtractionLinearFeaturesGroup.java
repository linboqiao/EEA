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

import java.io.File;
import java.util.TreeMap;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.exrel.features.re.linear.RelationExtractionLinearFeature;
import limo.exrel.utils.Dictionary;


public class RelationExtractionLinearFeaturesGroup extends FeaturesGroup<RelationExtractionLinearFeature> {
	private Dictionary dictionary = null;
	
	public RelationExtractionLinearFeaturesGroup(String id, File f) {
		super(id);
		dictionary = Dictionary.factory(f);
	}

	@Override
	public String extract(Object... args) {
		
		AbstractTree parseTree = (AbstractTree)args[0];
		Mention mention1 = (Mention)args[1];
		Mention mention2 = (Mention)args[2];
		
		Relation relation = (Relation)args[3];
		Sentence sentence = (Sentence)args[4];
	
		
		TreeMap<Integer, String> values = new TreeMap<Integer, String>(); 
		StringBuffer result = new StringBuffer();
		for (RelationExtractionLinearFeature feature : features) {
			String featureValue = feature.extract(parseTree, mention1, mention2, relation, sentence, this.getGroupId());
			if (featureValue == null) {
				continue;
			}
			
			if (feature.isBOWF() == true) {
				String[] parts = featureValue.split(RelationExtractionLinearFeature.BOWseparator);
				for (String p : parts) {
					featureValue= p;
					int index = dictionary.getIndex(feature, featureValue);			
					//System.out.println("feature val/index: "+ featureValue + " "+index);
					if (feature.isBoolean()) {
						values.put(index, "1");
					} else {
						//count
						if (values.containsKey(index)) {
							int count = Integer.parseInt(values.get(index));
							values.put(index, String.valueOf(count+1));
						} else {
							values.put(index,"1");
						}
						//values.put(index, featureValue);
					}
				}
			}
			else if (feature.containsConfidence()) {
				
			}
			else {
				int index = dictionary.getIndex(feature, featureValue);			
				//System.out.println("feature val/index: "+ featureValue + " "+index);
				if (feature.isBoolean()) {
					values.put(index, "1");
				} else {
					values.put(index, featureValue);
				}
			}
		}
		for (int index : values.keySet()) {
			if (result.length() > 0) {
				result.append(" ");				
			}
			result.append(index);
			result.append(":");
			result.append(values.get(index));
		}
		return result.toString();
	}
	
	@Override
	public void cleanup() {
		dictionary.save();
		dictionary.unload();
	}
}

