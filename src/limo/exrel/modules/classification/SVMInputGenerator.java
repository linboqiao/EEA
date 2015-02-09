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

import java.io.BufferedWriter;
import java.io.FileWriter;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.ClassSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.LabelMapping;
import limo.exrel.slots.OutputFileSlot;
import limo.exrel.slots.StringSlot;
import limo.exrel.utils.IndexLine;
import limo.exrel.utils.IndexReader;

public class SVMInputGenerator extends AbstractModule {
	
	public FileSlot examplesIdxFile = new FileSlot(true);
	public ClassSlot<LabelMapping> labelMappingClass = new ClassSlot<LabelMapping>(true);
	public StringSlot targetLabel = new StringSlot(true);
	public OutputFileSlot outExamplesFile = new OutputFileSlot(true);
	
	public SVMInputGenerator(String moduleId, String configId) {
		super(moduleId, configId);
	}

	@Override
	protected void _run() {		
		try {
			
			message("Reading examples from: %s", examplesIdxFile);
			IndexReader reader = new IndexReader(examplesIdxFile.get());
			
			message("Preparing examples file: %s", outExamplesFile.get().getAbsolutePath());
			BufferedWriter writer = new BufferedWriter(new FileWriter(outExamplesFile.get()));
			
			LabelMapping mapping = labelMappingClass.get().getConstructor(new Class[] {String.class}).newInstance(targetLabel.get());
			
			IndexLine line;
			int numLines = 0;
			while ((line = reader.readLine()) != null) {
				String[] temp = line.getData().split("\t");
				String currentLabel = temp[0];
				String data = temp[1];

				int label = -1;
				if (currentLabel != null) {
					label = mapping.map(currentLabel);
					//skip null
					//if (label == 0)
					//	continue;

				writer.write(String.format("%+1d %s", label, data));
				writer.newLine();

				numLines++;
				}
			}
			writer.close();
			reader.close();
			
			message("%d lines written to %s", numLines, outExamplesFile.get().getAbsolutePath());
			
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

	}

}
