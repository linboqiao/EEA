package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import org.mapdb.Fun;

import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/14/14
 * Time: 9:03 PM
 */
public class Utils {
    public static Span toSpan(ComponentAnnotation anno) {
        return new Span(anno.getBegin(), anno.getEnd());
    }

    public static int entityIdToInteger(String eid) {
        return Integer.parseInt(eid);
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

    public static boolean tfDfFilter(Fun.Tuple2<Integer, Integer> tfDf) {
        return tfDf == null || tfDfFilter(tfDf.a, tfDf.b);
    }

    public static boolean tfDfFilter(int tf, int df) {
        return documentFrequencyFilter(tf);
    }

    public static boolean termFrequencyFilter(int tf) {
        return tf / 10 == 0;
    }

    public static boolean documentFrequencyFilter(int df) {
        return df / 10 == 0;
    }
}
