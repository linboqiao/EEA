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
package limo.exrel.features;

import java.io.IOException;

import org.w3c.dom.Element;

public abstract class AbstractFeature {
	
	public static final String UNDEFINED = "::UNDEFINED::";
	
	public AbstractFeature() {}
	
	public void init(Element e) {
		_init(e);
	}
	
	/*
	 * Override to handle per-feature specific configuration options.
	 */
	protected void _init(Element e) {};
	
	public String getName() {
		return getClass().getSimpleName();
	}
	
	public String toString() {
		return getName();
	}
	
	public abstract String extract(Object... args) throws IOException;
	
}
