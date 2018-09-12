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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/*
 * Facility class to scan an index line-by-line.
 */
public class IndexReader {

	private File inputFile = null;
	private BufferedReader reader = null;
	
	
	public IndexReader(File f) {
		inputFile = Path.normalize(f);
	}
	
	public IndexLine readLine() {
		try {
			if (reader == null) {
				reader = new BufferedReader(new FileReader(inputFile));
			}
			String line = reader.readLine();
			if (line == null) {
				return null;
			}
			String data[] = line.split("\t", 2);
			return new IndexLine(data[0], data[1]);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void close() {
		try {
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
}
