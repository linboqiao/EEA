/* Updated by Barbara Plank
 * -----
 * 
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

package limo.exrel;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;

import java.util.Vector;

import org.w3c.dom.Element;

import limo.exrel.modules.AbstractModule;

import limo.exrel.slots.StringSlot;
import limo.exrel.slots.XMLConfigSlot;
import limo.exrel.utils.XMLConfig;
import limo.exrel.utils.XMLTools;

public class Exrel extends AbstractModule {

	// Modules indexed per id.
	private LinkedHashMap<String, AbstractModule> _modules = new LinkedHashMap<String, AbstractModule>();
	
	// Modules indexed per configuration section.
	private LinkedHashMap<String, Vector<AbstractModule>> _configModules = new LinkedHashMap<String, Vector<AbstractModule>>();
	
    public XMLConfigSlot exrelXMLConfigFile = new XMLConfigSlot(true);
	public StringSlot configurationName = new StringSlot(true);
	
	private static XMLConfig xmlconfig;
	
	String[] arguments;
	
	public Exrel() {
		super("Exrel","Main");
	}
	
	public Exrel(String xmlConfigFile, String configurationName) {
		super("Exrel","Main");
		arguments = new String[]{"--exrelXMLConfigFile", xmlConfigFile,"--configurationName",configurationName};	
	}

	@Override
	protected void _prepare() {
		xmlconfig = exrelXMLConfigFile.get();
		xmlconfig.isMainConfig(true); //set this to true such that a Temp dir is created if <TestDir> variable is not set
		
		
		//if (xmlconfig.useTempOutputDirectory()) {
		//	System.err.println("Using tmpDir: "+xmlconfig.getTemporaryOuputDirPath());
		//}
		
		Vector<Element>  configurations= xmlconfig.getConfigurations();
		if (configurations.size() == 0) {
			throw new RuntimeException("No configuration elements defined in XML config file");
		}
		for (Element config : configurations) {
			String configId = config.getAttribute("id");
			for (Element module : XMLTools.getChildren(config)) {
				String moduleClassName = module.getAttribute("class");
				String moduleId = module.getAttribute("id");
				String disableString = module.getAttribute("disable");
				boolean disable = false;
				if (disableString.equalsIgnoreCase("yes")) {
					disable = true;
				}
				addModule(configId, moduleClassName, moduleId, disable);
			}
		}
		if (!_configModules.keySet().contains(configurationName.get())) {
			throw new RuntimeException(String.format("Undefined configuration id: %s", configurationName));
		}
	}
	
	public String getSlotValue(String moduleId, String slotName) {
		if (_modules.get(moduleId) == null) {
			throw new RuntimeException(String.format("No such module id defined: %s", moduleId));
		}
		return _modules.get(moduleId).getSlot(slotName).toString();
	}

	
	@SuppressWarnings("unchecked")
	public void addModule(String configId, String moduleClassName, String moduleId, boolean disable) {		
		if (!_modules.keySet().contains(moduleId)) {
			try {
				@SuppressWarnings("rawtypes")
				Class moduleClass = Class.forName(moduleClassName);
				AbstractModule module = 
					(AbstractModule) moduleClass.getConstructor(Class.forName("java.lang.String"),Class.forName("java.lang.String")).newInstance(moduleId,configId);
					
					//getConstructor(Class.forName("java.lang.String")).
					
				//module.init(this, exrelXMLConfigFile.get());
				//keep just one xmlConfig object!
				module.init(this, xmlconfig);
				module.setEnabled(!disable);
				_modules.put(moduleId, module);
				if (_configModules.get(configId) == null) {
					_configModules.put(configId, new Vector<AbstractModule>());
				}
				_configModules.get(configId).add(module);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		} else {
			throw new RuntimeException("Duplicate module id: " + moduleId);
		}
	}
	
	public static void main(String[] args) {
		new Exrel()._main(args);
	}
	
	
	public void process() {
		this.main(arguments);
	}

	@Override
	protected void _run() {
		message("Running configuration: %s", configurationName.get());
		for (AbstractModule module : _configModules.get(configurationName.get())) {
			if (module.isEnabled()) {
				module.run();
				try {
					module.join();
					Thread.yield();
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
			}
		}
	}

	@Override
	public void _cleanUp() {
		if (xmlconfig.useTempOutputDirectory()) {
			message("Cleaning up tmp directory... " +xmlconfig.getTemporaryOuputDirPath());
			try {
				xmlconfig.deleteTempOutputDirectory();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
