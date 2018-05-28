package edu.cmu.cs.lti.cr.readers.annotated_nyt;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 8/27/14
 * Time: 4:19 PM
 */
public class Decompressor {
    public static void main(String[] args) throws IOException {
        String inputDir = args[1];
        String outputDir = args[0];

        for (File tarFile : FileUtils.listFiles(new File(inputDir), new SuffixFileFilter(".tgz"), TrueFileFilter.INSTANCE)) {
            TarArchiveInputStream tarStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tarFile)));
            TarArchiveEntry entry = null;
            String individualFiles;
            int offset;
            FileOutputStream outputFile = null;
            while ((entry = tarStream.getNextTarEntry()) != null) {
                if (entry.isFile()) {
                    individualFiles = entry.getName();
                    byte[] content = new byte[(int) entry.getSize()];
                    offset = 0;
                    tarStream.read(content, offset, content.length - offset);
                    File outFile = new File(outputDir + "/" + individualFiles);
                    File parent = outFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IllegalStateException("Couldn't create dir: " + parent);
                    }
                    outputFile = new FileOutputStream(outFile);
                    IOUtils.write(content, outputFile);
                    outputFile.close();
                }
            }
            tarStream.close();
        }
    }
}