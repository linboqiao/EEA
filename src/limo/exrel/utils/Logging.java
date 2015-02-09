/*
 * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
 *
 * From ExRel0.9 software
 * Imported 2011, by Barbara Plank
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

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging {

	public static void message(Object caller, String template, Object... args) {
		log(System.out, caller, template, args);
	}
	
	public static void error(Object caller, String template, Object... args) {
		log(System.err, caller, template, args);
	}
	
	public static void log(PrintStream stream, Object caller, String template, Object... args) {
		stream = System.err;
		stream.println(
				" " + 
				new SimpleDateFormat ("yyyy-MM-dd@HH:mm:ss").format(new Date(System.currentTimeMillis())) + " [" +
				caller.getClass().getSimpleName() + "] " + 
				String.format(template, args));
	}
	
}
