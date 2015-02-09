/* Updated by Barbara Plank
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
package limo.exrel.features.re.structured;

import java.io.IOException;
import java.util.ArrayList;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.AbstractTree;
import limo.exrel.features.AbstractFeature;


public abstract class RelationExtractionStructuredFeature extends AbstractFeature  {

	@SuppressWarnings("unchecked")
	@Override
	public String extract(Object... args) throws IOException {
		return _extract((AbstractTree)args[0], (Mention)args[1], (Mention)args[2], (Relation)args[3], (Sentence)args[4], (ArrayList<Object>)args[5]);
	}
	
	protected abstract String _extract(AbstractTree parseTree, Mention mention1, Mention mention2, Relation relation, Sentence sentence, ArrayList<Object> resources) throws IOException;
	
	// get minimum number from span indices
	public static int max(int[] tokenIds1, int[] tokenIds2) {
		int[] all = getCombined(tokenIds1, tokenIds2);
		int max = all[0];
		for (int x = 0; x < all.length; x++)
			if (all[x] > max)
				max = all[x];
		return max;
	}

	protected static int[] getCombined(int[] tokenIds1, int[] tokenIds2) {
		int[] all = new int[tokenIds1.length + tokenIds2.length];
		for (int i = 0; i < tokenIds1.length; i++) {
			all[i] = tokenIds1[i];
		}
		int startIdx = tokenIds1.length;
		for (int i = 0; i < tokenIds2.length; i++) {
			all[i + startIdx] = tokenIds2[i];
		}
		return all;
	}

	public static int min(int[] tokenIds1, int[] tokenIds2) {
		int[] all = getCombined(tokenIds1, tokenIds2);
		int min = all[0];
		for (int x = 0; x < all.length; x++)
			if (all[x] < min)
				min = all[x];
		return min;
	}
}
