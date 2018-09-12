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
package limo.exrel.modules;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import limo.exrel.Exrel;
import limo.exrel.slots.Slot;
import limo.exrel.utils.Logging;
import limo.exrel.utils.XMLConfig;


public abstract class AbstractModule extends Thread {

	private String _moduleId = null;
	private String _configId = null;
	public boolean _enabled = true;
	
	public void setEnabled(boolean enable) {
		if (!enable) {
			message("Disabling module");
		}
		_enabled = enable;
	}
	
	public boolean isEnabled() {
		return _enabled;
	}
	
	protected LinkedHashMap<String, Slot<?>> getSlots() {
		LinkedHashMap<String, Slot<?>> res = new LinkedHashMap<String, Slot<?>>();
		try {
			for (Field f : getClass().getFields()) {
				if (f.get(this) instanceof Slot<?>) {
					res.put(f.getName(), (Slot<?>)f.get(this));
				}
			}
			return res;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void checkInitConsistency() {
		LinkedHashMap<String, Slot<?>> slots = getSlots();
		for (String name : slots.keySet()) {
			Slot<?> s = slots.get(name);
			if (s.isMandatory() && s.toString() == null) {
				throw new RuntimeException(
						String.format("Missing value for mandatory slot %s in module %s", name, getClass().getSimpleName()));
			}
			if (s.toString() != null) {
				Class<?> failedCheck =s.runChecks(); 
				if (failedCheck != null) {
					throw new RuntimeException(
							String.format("Check \"%s\" failed by value \"%s\" of slot \"%s\" of module \"%s\"", 
									failedCheck.getSimpleName(),
									s.toString(),
									name,
									getClass().getSimpleName()));
				}
			}
		}
	}
	
	public Slot<?> getSlot(String name) {
		LinkedHashMap<String, Slot<?>> slots = getSlots();
		for (String slotname : slots.keySet()) {
			if (slotname.equals(name)) {
				return slots.get(slotname);
			}
		}
		throw new RuntimeException(
				String.format("No such slot %s defined for module %s", name, getClass().getSimpleName()));
	}
	
	public void message(String template, Object... args) {
		Logging.message(this, "(" + getModuleId() + ") " + template, args);
	}
	
	public AbstractModule(String moduleId, String configId) {
		_moduleId = moduleId;
		_configId = configId;
	}

	
	public void init(Exrel relationExtractor, XMLConfig conf) {
		Element e = conf.getModuleElement(getClass().getName(), _moduleId);
		message("Initializing");
		NodeList params = e.getElementsByTagName("slot");
		Element param;
		String name;
		String type;
		String source;
		String value;
		for (int i=0; i<params.getLength(); i++) {			
			param = (Element)params.item(i);
			value = conf.parse(param.getTextContent(), e);
			name = conf.parse(param.getAttribute("name"), e);
			type = conf.parse(param.getAttribute("type"), e);
			if (type.equals("value")) {
				this.setSlot(name, value);
			} else if (type.equals("slot")) {
				source = conf.parse(param.getAttribute("source"), e);
				if (source == null || source.length() == 0) {
					throw new RuntimeException(
							String.format(
									"No source defined for slot \"%s\" of module \"%s\"", 
									name, e.getAttribute("id")));
				}
				String slotValue = relationExtractor.getSlotValue(source, value);
				if (slotValue == null) {
					throw new RuntimeException(
							String.format("No value for slot \"%s\" of source \"%s\" has been set yet", slotValue, source));
				}
				this.setSlot(name, slotValue);
			} else {
				throw new RuntimeException("Unknown slot value type: " + type);
			}
		}
		_init(relationExtractor, e);
		checkInitConsistency();
	}	
	
	/*!
	 * \brief Run method of a module.
	 * 
	 * \return true on success, false otherwise.
	 */
	public final void run() {
		message("Running");
		checkInitConsistency();
		_prepare();
		try {
			_run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		_cleanUp();
	}
	
	public String getModuleId() {
		return _moduleId;
	}
	
	public String getConfigId() {
		return _configId;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof AbstractModule)) {
			return false;
		}
		return this.getModuleId().equals(((AbstractModule)o).getModuleId());
	}
	
	public void setSlot(String slotName, String slotValue) {
		try {
			((Slot<?>)getClass().getField(slotName).get(this)).setString(slotValue);
		} catch (Exception ex) {
			throw new RuntimeException(
					String.format("No such slot %s defined for module %s", slotName, getClass().getSimpleName()));
		}
	}
	
	/* Executed after the init method invoked by exrel.
	 * 
	 * Override to allow a module to define specific configuration options
	 * in the initialization xml file.
	 */
	protected void _init(Exrel system, Element e) {}
	
	/*!
	 * Executed immediatly before _run is invoked.
	 * 
	 * Override to define pre-execution checks and initialization.
	 */
	protected void _prepare() {}
	
	/*!
	 * Executed immediatly after _run is invoked.
	 * 
	 * Override to define post-execution checks and environment cleanup.
	 */
	public void _cleanUp() {}
	
	/*!
	 * Implementation of the module algorithms.
	 */
	protected abstract void _run() throws Exception;	
	
	
	protected void _main(String[] args) {		
		try {
			HashMap<String, Slot<?>> slots = getSlots();
			if (slots.size() == 0) {
				run();
				join();
				return;
			}
			if (args.length == 0 || args.length%2 != 0) {
				_help();
			}
			for (int i=0; i<args.length; i+=2) {
				if (!args[i].matches("^--.+")) {
					_help();
				}
				try {
					setSlot(args[i].substring(2), args[i+1]);
				} catch (Exception ex) {
					ex.printStackTrace();
					_help();
				}
			}
			try { 
				checkInitConsistency();
			} catch (Exception ex) {
				ex.printStackTrace();
				_help();
			}
			run();
			join();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	protected void _help() {
		HashMap<String, Slot<?>> slots = getSlots();
		System.err.println();
		System.err.printf(" The module %s defines the following slots:\n\n", getClass().getSimpleName());
		for (String name: slots.keySet()) {
			Slot<?> s = slots.get(name);
			System.err.printf("  %s--%s <%s%s>%s\n",
					s.isMandatory() ? " " : "[",
					name, 					
					s.getClass().getSimpleName(),
					s.toString() != null ? " = " + s.toString() : "",
					s.isMandatory() ? " " : "]"
			);
		}
		System.err.println();
		System.exit(1);
	}
	
}
