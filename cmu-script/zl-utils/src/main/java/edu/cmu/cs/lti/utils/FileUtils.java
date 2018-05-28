/**
 *
 */
package edu.cmu.cs.lti.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * @author zhengzhongliu
 */
public class FileUtils {
    public static boolean ensureDirectory(String path) {
        File dir = new File(path);
        return ensureDirectory(dir);
    }

    public static boolean ensureDirectory(File dir) {
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    public static File[] getFilesWithSuffix(File dir, final String suffix) {
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(suffix);
            }
        });
    }

    public static String joinPaths(String... directories) {
        return joinPathsAsFile(directories).getPath();
    }

    public static File joinPathsAsFile(String... directories) {
        if (directories.length == 1) {
            return new File(directories[0]);
        } else {
            String[] rest = Arrays.copyOfRange(directories, 1, directories.length);
            return new File(directories[0], joinPathsAsFile(rest).getPath());
        }
    }
}
