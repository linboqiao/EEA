package limo.io.ry;

import java.io.File;
import java.io.FilenameFilter;

/***
 * FileNameFilter
 * @author Barbara Plank
 *
 */
public final class RothYihConll2004FilenameFilter implements FilenameFilter {
	  
    private String suffix;

    /**
    * Construction method
    */
    public RothYihConll2004FilenameFilter(String suffix) {
        this.suffix = suffix;
    }
    
    public RothYihConll2004FilenameFilter() {
        this.suffix = ".corp";
    }

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   dir    the directory in which the file was found.
     * @param   name   the name of the file.
     * @return  <code>true</code> if and only if the name should be
     * included in the file list; <code>false</code> otherwise.
     */
    public boolean accept(File dir, String name) {
        if (name.endsWith(suffix))
            return true;
        return false;
    }
}