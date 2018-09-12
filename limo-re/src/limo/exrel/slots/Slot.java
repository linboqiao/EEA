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
package limo.exrel.slots;

import java.util.HashSet;

import limo.exrel.checks.SlotCheck;

public abstract class Slot<T> {
	
	//TODO Add a description field to Slots
	//TODO Armonize constructors, like (mandatory, defaultvalue, description)
	//TODO have AbstractModule use description when printing a module's help.
	
	private String strVal = null;
	private T val = null;
	private boolean mandatory = true;
	private HashSet<SlotCheck<T>> checks = new HashSet<SlotCheck<T>>();
	
	public Slot() {
		addDefaultChecks();
	}
	
	public Slot(boolean mandatory) {
		this();
		setMandatory(mandatory);
	}
	
	public Slot(String value) {
		this();
		setString(value);
	}
	
	public Slot(String value, boolean mandatory) {
		this();
		setString(value);
		setMandatory(mandatory);
	}
	
	public void setString(String value) {
		strVal = value;
		val = null;
	}
	
	public void set(T value) {
		val = value;
		strVal = val.toString();
	}
	
	public void set(Slot<T> slot) {
		val = slot.get();
		strVal = val.toString();
	}
	
	public String toString() {
		return strVal;
	}
	
	public T get() {
		if (val != null) {
			return val;
		}
		val = evaluate();
		return val;
	}
	
	public void setMandatory(boolean m) {
		this.mandatory = m;
	}
	
	public boolean isMandatory() {
		return mandatory;
	}
	
	public void addCheck(SlotCheck<T> check) {
		checks.add(check);
	}
	
	/* Return the class of the first failed check */
	public Class<?> runChecks() {
		for (SlotCheck<T> sc : checks) {
			if (!sc.check(get())) {
				return sc.getClass();
			}
		}
		return null;
	}
	
	protected abstract void addDefaultChecks();
	protected abstract T evaluate();
}
