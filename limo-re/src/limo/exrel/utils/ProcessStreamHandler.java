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

import java.io.*;

public class ProcessStreamHandler extends Thread
{

	public ProcessStreamHandler(InputStream in, OutputStream out) {
		m_in = in;
		m_out = out;
	}

	public void run() {
		try {
			InputStreamReader reader = new InputStreamReader(m_in);
			PrintWriter writer = m_out == null ? null : new PrintWriter(m_out);
			int read;
			int count = 0;
			while((read = reader.read()) != -1) {
				if(writer != null) {
					writer.write(read);
					if (++count%500 == 0) {
						writer.flush();
						count = 0;
					}
				}
			}
			if(writer != null) {
				writer.flush();
				writer.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static Process handle(Process proc, OutputStream out, OutputStream err) {
		try {
			ProcessStreamHandler errHandler = new ProcessStreamHandler(proc.getErrorStream(), err);
			ProcessStreamHandler outHandler = new ProcessStreamHandler(proc.getInputStream(), out);
			errHandler.start();
			outHandler.start();
			proc.waitFor();
			errHandler.join();
			outHandler.join();
			Thread.yield();
			err.flush();			
			out.flush();
			err.close();
			out.close();
			return proc;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Process handle(Process proc, String outFile, String errFile) {
		try {
			FileOutputStream out;
			FileOutputStream err;
			out = null;
			err = null;
			if(outFile != null) {
				outFile = (new File(outFile)).getAbsolutePath();
				(new File(outFile)).getParentFile().mkdirs();
				out = new FileOutputStream(outFile);
			}
			if(errFile != null) {
				errFile = (new File(errFile)).getAbsolutePath();
				(new File(errFile)).getParentFile().mkdirs();
				err = new FileOutputStream(errFile);
			}
			return handle(proc, out, err);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private InputStream m_in;
	private OutputStream m_out;
}
