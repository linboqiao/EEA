/*
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
package limo.exrel.utils;

import java.util.Arrays;

public class IndexKey {
	
	Integer[] values;
	
	public IndexKey() {
		values = new Integer[0];
	}
	
	public IndexKey(int numKeys) {
		values = new Integer[numKeys];
	}
	
	public IndexKey(Integer[] v) {
		values = v;
	}
	
	public int size() {
		return values.length;
	}
	
	public void set(int index, Integer value) {
		values[index] = value;
	}
	
	public Integer get(int index) {
		return values[index];
	}
	
	public IndexKey sample(Integer... indices) {
		IndexKey result = new IndexKey();
		for (Integer i : indices) {
			result = result.append(get(i));
		}
		return result;
	}
	
	public IndexKey subset(int start, int count) {
		if (start < 0 || count < 0 || start + count > size()) {
			throw new RuntimeException(String.format("Invalid parameters start:%d count:%d", start, count));
		}
		IndexKey result = new IndexKey(count);
		for (int i=start; i<start+count; i++) {
			result.set(i-start, get(i));
		}
		return result;
	}
	
	public IndexKey append(Integer... values) {
		Integer[] result = new Integer[size() + values.length];
		int i=0;
		for (i=0; i<this.values.length; i++) {
			result[i] = this.values[i];
		}
		for (i=0; i<values.length; i++) {
			result[this.values.length + i] = values[i];
		}
		return new IndexKey(result);
	}
	
	boolean matches(IndexKey query) {
		for (int i=0; i<query.size(); i++) {
			if (query.get(i) != null && !query.get(i).equals(this.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Integer i: values) {
			if (result.length() > 0) {
				result.append(" ");
			}
			result.append(i);
		}
		return result.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Integer[]) {
			return Arrays.equals(((Integer[])obj), values);
		} else if (obj instanceof IndexKey) {
			return Arrays.equals(((IndexKey)obj).values, values);
		} else {
			throw new RuntimeException("You're comparing bananas to pineapples!");
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
	
	public static IndexKey parse(String key) {
		IndexKey result = new IndexKey();
		for (String keyValue : key.split(" ")) {
			result = result.append(Integer.parseInt(keyValue));
		}
		return result;
	}
	
}

