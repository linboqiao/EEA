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


public class Separator extends FeaturesGroup<AbstractFeature> {
	String sep;
	public Separator(String id, String sep) {
		super(id);
		this.sep = sep;
	}
	@Override
	public String extract(Object... args) {
		return sep;
	}
	@Override
	public void addFeature(AbstractFeature feature) {
		throw new RuntimeException("It is not possible to add features to a separator");
	}
}