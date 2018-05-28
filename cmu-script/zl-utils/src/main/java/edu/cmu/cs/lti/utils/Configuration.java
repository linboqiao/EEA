package edu.cmu.cs.lti.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/25/14
 * Time: 1:17 AM
 */
public class Configuration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Properties properties;

    private File configFile;

    public Configuration() {
        properties = new Properties();
    }

    public Configuration(String configurationFilePath) throws IOException {
        this(new File(configurationFilePath));
    }

    public Configuration(File configurationFile) throws IOException {
        if (!configurationFile.exists()) {
            throw new FileNotFoundException("Cannot read config file at : " + configurationFile.getCanonicalPath());
        }
        properties = new Properties();
        properties.load(new FileInputStream(configurationFile));

        this.configFile = configurationFile;
    }

    public void add(Object key, Object value) {
        properties.put(key, value);
    }


    public Set<Map.Entry<Object, Object>> getAllEntries() {
        return properties.entrySet();
    }

    public String getOrElse(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String get(String key) {
        return getOrElse(key, null);
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        String value = getOrElse(key, null);
        if (value == null) {
            return defaultVal;
        }
        return value.equalsIgnoreCase("true");
    }

    public int getInt(String key, int defaultVal) {
        String value = get(key);
        if (value == null) {
            return defaultVal;
        }
        return Integer.parseInt(value);
    }

    public long getLong(String key, long defaultVal) {
        String value = get(key);
        if (value == null) {
            return defaultVal;
        }
        return Long.parseLong(value);

    }

    public double getDouble(String key, double defaultVal) {
        String val = get(key);
        if (val != null) {
            return Double.parseDouble(get(key));
        } else {
            return defaultVal;
        }
    }

    public File getFile(String key) {
        String val = get(key);
        if (val == null) {
            return null;
        }
        return new File(val);
    }

    public String[] getList(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return new String[0];
        }

        return value.split("[,\\s]+");
    }

    public int[] getIntList(String key) {
        String value = get(key);
        if (value == null) {
            return new int[0];
        }

        String[] strs = value.split("[,\\s]+");

        int[] results = new int[strs.length];

        for (int i = 0; i < strs.length; i++) {
            results[i] = Integer.parseInt(strs[i]);
        }

        return results;
    }

    public File getConfigFile() {
        return configFile;
    }

}