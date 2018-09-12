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
package limo.exrel.modules.classification;

import limo.exrel.utils.ProcessStreamHandler;

import java.io.File;
import java.io.FileOutputStream;

public class SVMClassify {
	
	File path;
	File examples;
	File model;
	File predictions;
	File output;
	File error;

	public SVMClassify(File binary) {
		this.path = binary;
	}
	
	public void setParameters(File examples, File model, File predictions, File output, File error) {
		this.examples = examples;
		this.model = model;
		this.predictions = predictions;
		this.output = output;
		this.error = error;
	}
	
	public String getCommandString() {
		return String.format("%s %s %s %s", 
				path.getAbsolutePath(), 
				examples.getAbsolutePath(), 
				model.getAbsolutePath(), 
				predictions.getAbsolutePath());
	}
	
	public void run() {
		try {
			ProcessStreamHandler.handle(
					Runtime.getRuntime().exec(getCommandString()),
					new FileOutputStream(output), 
					new FileOutputStream(error)).waitFor();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
