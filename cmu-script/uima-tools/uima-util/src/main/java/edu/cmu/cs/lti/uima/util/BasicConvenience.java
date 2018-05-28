/**
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corp. 2007
 * All rights reserved.
 */
package edu.cmu.cs.lti.uima.util;

import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Assortment of various functions. This class contains both basic convenience methods that ONLY
 * DEPEND ON THE JAVA JRE CLASSES.
 *
 * @author Jun Araki
 * @version: 0.1
 */
public class BasicConvenience {

    /**
     * Process a command-line argument array.
     *
     * @param args   a string array
     * @param option
     * @return
     */
    public static String processArgs(String args[], String option, String def) {
        for (int i = 0; i < args.length; i++)
            if (args[i].equals(option))
                return args[i + 1];
        return def;
    }

    /**
     * Return true if the option appears in the args
     *
     * @param args
     * @param option
     * @return
     */
    public static boolean isOptionPresent(String args[], String option) {
        for (int i = 0; i < args.length; i++)
            if (args[i].equals(option))
                return true;
        return false;
    }

    /**
     * @param fullType A fully qualified class name, e.g., com.ibm.sai.Foo
     * @return Everything after the last '.' in the argument, e.g., Foo
     */
    public static String removeClassPath(String fullType) {
        return removePrefix(fullType, '.');
    }

    /**
     * @param fullType A fully qualified class name, e.g., com.ibm.sai.Foo
     * @return Everything up to the last '.' in the argument, e.g., com.ibm.sai
     */
    public static String getClassPath(String fullType) {
        return removeSuffix(fullType, '.');
    }

    /**
     * @param fullType A URI or file name, e.g., file:foo/bar
     * @return Everything after the last file seperator and/or ':', e.g., bar
     */
    public static String removeFilePath(String fullType) {
        return removePrefix(removePrefix(removePrefix(fullType, ':'), '/'), '\\');
    }

    /**
     * Remove a prefix before the last occurrence of a separator.
     *
     * @param fullName  the full name to remove the prefix from (e.g., bob.dole.txt)
     * @param separator the separator (e.g., '.')
     * @return the part of the fullName after last occurrence of the separator (e.g., txt), or the
     * fullName if the separator do not occur in fullName
     */
    public static String removePrefix(String fullName, char seperator) {
        int charPos = fullName.lastIndexOf(seperator);
        if (charPos == -1)
            return fullName;
        else
            return fullName.substring(charPos + 1);
    }

    public static String readFile(File filename, String encoding, String sub) throws IOException {
        BufferedReader reader = null;
        String fileText;
        try {
            if (encoding == null)
                reader = new BufferedReader(new FileReader(filename));
            else if (sub == null)
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
            else
                reader = getEncodingSafeBufferedReader(filename, encoding, "-");
            fileText = read(reader);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return fileText;
    }

    public static String readFile(File filename) throws IOException {
        return readFile(filename, null, null);
    }

    public static String readFile(File filename, String encoding) throws IOException {
        return readFile(filename, encoding, null);
    }

    /**
     * Read a file by mapping it completely into memory.
     *
     * @param filename
     * @return the bytes contained in the file, as a string, in the specified encoding.
     * @throws IOException
     */
    public static String fastReadFile(File file, String encoding) throws IOException {
        return new String(fastReadFile(file), encoding);
    }

    /**
     * Read a file by mapping it completely into memory.
     *
     * @param filename
     * @return the bytes contained in the file
     * @throws IOException
     */
    public static byte[] fastReadFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            MappedByteBuffer buffer = stream.getChannel().map(MapMode.READ_ONLY, 0, file.length());
            byte[] bytes = new byte[(int) file.length()];
            buffer.get(bytes);
            return bytes;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public static byte[] readFileBytes(File file) throws IOException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        try {
            byte[] bytes = new byte[(int) file.length()];
            int off = 0;
            while (off < bytes.length) {
                int read = stream.read(bytes, off, bytes.length - off);
                off += read;
            }
            return bytes;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public static String read(Reader reader) throws IOException {
        StringBuilder buf = new StringBuilder();

        int readInt;

        while ((readInt = reader.read()) != -1)
            buf.append((char) readInt);

        return buf.toString();
    }

    /**
     * Remove a suffix (e.g., ".txt") after the last occurrence of a separator (e.g., "."). This
     * duplicates functionality in
     * {@link org.apache.commons.lang.StringUtils#substringAfterLast(String, String)}.
     *
     * @param fullName  the full name to remove the suffix
     * @param separator the separator
     * @return the fullName without the last occurrence of the separator, or the fullName if the
     * separator do not occur in fullName
     */
    public static String removeSuffix(String fullName, char separator) {
        int charPos = fullName.lastIndexOf(separator);
        if (charPos == -1)
            return fullName;
        else
            return fullName.substring(0, charPos);
    }

    public static double harmonicMean(List<Double> nums) {
        double retval;
        double denominator = 0.0;
        for (double c : nums) {
            if (c == 0.0)
                return 0.0;

            denominator += 1 / c;
        }
        retval = (nums.size() / denominator);
        return retval;
    }

    public static double product(List<Double> nums) {
        double retval = 1;
        for (double c : nums) {
            retval *= c;
        }
        return retval;
    }

    /**
     * Check that a map (usually a Properties instance) has certain keys defined.
     *
     * @param the map to check the keys
     * @param the keys to check
     * @throws IllegalArgumentException if any of the keys is missing
     */
    public static void checkDefinedKeys(Map map, String... keys) throws IllegalArgumentException {
        for (String key : keys)
            if (!map.containsKey(key))
                throw new IllegalArgumentException("Undefined mandatory key '" + key + "'");
    }

    /**
     * Returns a reader for the specified file using the specified encoding. Any characters in the
     * input that are malformed or invalid for that encoding are replaced with the specified
     * substitution string.
     *
     * @param f
     * @param encoding
     * @param substitution
     * @return
     * @throws FileNotFoundException
     */
    public static BufferedReader getEncodingSafeBufferedReader(File f, String encoding,
                                                               String substitution) throws FileNotFoundException {
        BufferedReader cin;
        Charset cs = Charset.forName(encoding);
        CharsetDecoder decoder = cs.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        decoder.replaceWith(substitution);

        InputStream is = new FileInputStream(f);
        if (f.toString().endsWith(".zip"))
            is = new ZipInputStream(is);

        cin = new BufferedReader(new InputStreamReader(is, decoder));
        return cin;
    }

    public static String makeStringFromCharacters(int... cs) {
        int size = cs.length;
        byte[] arr = new byte[size];
        for (int i = 0; i < size; i++) {
            arr[i] = (byte) cs[i];
        }
        return new String(arr);
    }

    /**
     * Elide a string to a max size, by chopping out the middle
     * and substituting ... for that.
     *
     * @param s
     * @param length, a number bigger than 10
     * @return the elided string
     */
    public static String elideString(String s, int length) {
        if (null == s) {
            s = "";
        }
        int sLength = s.length();

        if (length >= sLength) {
            return s;
        }

        int elideSize = (sLength + 3 - length) / 2;  // always positive, add 3 to compensate for ...
        int startElide = 1 + (sLength / 2) - elideSize;
        int endElide = (sLength / 2) + elideSize;
        return s.substring(0, startElide) + "..." + s.substring(endElide);
    }

    public static int[] ArrayOfIntegers2intArray(ArrayList<Integer> in) {
        int[] result = new int[in.size()];
        for (int i = 0; i < in.size(); i++) {
            result[i] = in.get(i);
        }
        return result;
    }

    /**
     * Given 2 arrays which may be arrays of other things,
     * determine if they have exactly the same dimensions, to some level
     *
     * @param a1
     * @param a2
     * @param levels 1 means an array X[], 2 means X[] [], etc.
     * @return true if all dimensions are the same
     */
    public static boolean allArrayDimensionsEqual(Object a1, Object a2, int levels) {
        int length = Array.getLength(a1);
        if (length != Array.getLength(a2)) {
            return false;
        }

        if (levels > 1) {
            for (int i = 0; i < length; i++) {
                if (!allArrayDimensionsEqual(Array.get(a1, i), Array.get(a2, i), levels - 1)) {
                    return false;
                }
            }
        }
        return true;
    }


    private static final long MB = 1024 * 1024;

    public static void runAndReportMemoryUsage(String label, Runnable r) {
        System.gc();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.gc();
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        System.out.println("STARTING " + label);
        System.out.println("Total (JVM): " + total / MB + " MB");
        System.out.println("Free (before): " + free / MB + " MB");
        long used = total - free;
        long usedAtStart = used;
        System.out.println("Used (before): " + used / MB + " MB");
        r.run();
        System.out.println("DONE " + label);
        System.gc();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.gc();

        total = Runtime.getRuntime().totalMemory();
        free = Runtime.getRuntime().freeMemory();
        long nowUsed = total - free;
        System.out.println("Free (after): " + free / MB + " MB");
        System.out.println("Used (after): " + nowUsed / MB + " MB");
        System.out.println("Difference: " + (nowUsed - usedAtStart) / MB + " MB");
    }

    public static void printMemInfo(Logger logger) {
        printMemInfo(logger, "");
    }

    public static void printMemInfo(Logger logger, String msg) {
        // Get current size of heap in bytes
        double heapSize = Runtime.getRuntime().totalMemory() / (double) (1024 * 1024);

        //Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        //Any attempt will result in an OutOfMemoryException.
        double heapMaxSize = Runtime.getRuntime().maxMemory() / (double) (1024 * 1024);

        // Get amount of free memory within the heap in bytes. This size will increase
        // after garbage collection and decrease as new objects are created.
        double heapFreeSize = Runtime.getRuntime().freeMemory() / (double) (1024 * 1024);

        logger.info(String.format("%s. Heap size: %.2f MB, Max Heap Size: %.2f MB, Free Heap Size: %.2f MB, Used Memory: %.2f MB", msg, heapSize, heapMaxSize, heapFreeSize, heapSize - heapFreeSize));
    }

}
