package edu.cmu.cs.lti.utils;

import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/20/14
 * Time: 3:43 PM
 */
public class DebugUtils {
    public static String getMemInfo() {
        return getMemInfo("");
    }

    public static String getMemInfo(String msg) {
        // Get current size of heap in bytes
        double heapSize = Runtime.getRuntime().totalMemory() / (double) (1024 * 1024);

        //Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        //Any attempt will result in an OutOfMemoryException.
        double heapMaxSize = Runtime.getRuntime().maxMemory() / (double) (1024 * 1024);

        // Get amount of free memory within the heap in bytes. This size will increase
        // after garbage collection and decrease as new objects are created.
        double heapFreeSize = Runtime.getRuntime().freeMemory() / (double) (1024 * 1024);

        return String.format("%s. Heap size: %.2f MB, Max Heap Size: %.2f MB, Free Heap Size: %.2f MB, Used " +
                "Memory: %.2f MB", msg, heapSize, heapMaxSize, heapFreeSize, heapSize - heapFreeSize);
    }

    /**
     * Pause when it is in debug mode.
     *
     * @param logger
     */
    public static void pause(Logger logger) {
        if (logger.isDebugEnabled()) {
            logger.debug("Press enter to continue...");
            try {
                System.in.read();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Always pause.
     */
    public static void pause() {
        System.out.println("Press enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
        }
    }
}
