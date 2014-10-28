package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;

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
        // Get current size of heap in bytes
        double heapSize = Runtime.getRuntime().totalMemory() * 10e-6;

        //Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        //Any attempt will result in an OutOfMemoryException.
        double heapMaxSize = Runtime.getRuntime().maxMemory() * 10e-6;

        // Get amount of free memory within the heap in bytes. This size will increase
        // after garbage collection and decrease as new objects are created.
        double heapFreeSize = Runtime.getRuntime().freeMemory() * 10e-6;

        logger.info(String.format("Heap size: %.2f MB, Max Heap Size: %.2f MB, Free Heap Size: %.2f MB", heapSize, heapMaxSize, heapFreeSize));
    }
}
