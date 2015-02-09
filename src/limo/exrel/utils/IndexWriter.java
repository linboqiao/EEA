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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/*
 * Facility class to scan an index line-by-line.
 */
public class IndexWriter {

	private File outputFile = null;
	private BufferedWriter writer = null;
	
	
	public IndexWriter(File f) {
		outputFile = Path.normalize(f);
	}
	
	public void write(IndexKey key, String data) {
		try {
			if (writer == null) {
				writer = new BufferedWriter(new FileWriter(outputFile));
			}
			writer.write(key.toString());
			writer.write("\t");
			writer.write(data);
			writer.write("\n");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void flush() {
		if (writer == null) {
			return;
		}
		try {
			writer.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public void close() {
		if (writer == null) {
			return;
		}
		try {
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
}
