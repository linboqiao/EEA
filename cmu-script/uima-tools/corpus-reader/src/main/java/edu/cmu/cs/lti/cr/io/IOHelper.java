package edu.cmu.cs.lti.cr.io;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 8/27/14
 * Time: 9:35 PM
 */
public class IOHelper {
    public static void writeFile(File outFile, String content) throws IOException {
        File parent = outFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
        FileOutputStream outputStream = new FileOutputStream(outFile);
        IOUtils.write(content, outputStream);
        outputStream.close();
    }
}
