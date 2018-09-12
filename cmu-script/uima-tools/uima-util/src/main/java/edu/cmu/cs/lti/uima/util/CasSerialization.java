package edu.cmu.cs.lti.uima.util;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 10:33 AM
 *
 * @author Zhengzhong Liu
 */
public class CasSerialization {
    private static final Logger logger = LoggerFactory.getLogger(CasSerialization.class);


    /**
     * Serialize a CAS to a file in XMI format
     *
     * @param aCas       CAS to serialize
     * @param outputFile output file
     * @throws SAXException
     * @throws Exception
     * @throws ResourceProcessException
     */
    public static void writeAsGzip(CAS aCas, File outputFile) throws IOException, SAXException {
        GZIPOutputStream gzipOut = null;

        try {
            // write gzipped XMI
            gzipOut = new GZIPOutputStream(new FileOutputStream(outputFile));
            XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
            XMLSerializer xmlSer = new XMLSerializer(gzipOut, false);
            ser.serialize(aCas, xmlSer.getContentHandler());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (gzipOut != null) {
                gzipOut.close();
            }
        }
    }

    /**
     * Serialize a CAS to a file in XMI format
     *
     * @param aCas    CAS to serialize
     * @param xmiFile output file
     * @throws SAXException
     * @throws Exception
     * @throws ResourceProcessException
     */
    public static void writeAsXmi(CAS aCas, File xmiFile) throws IOException, SAXException {
        FileOutputStream out = null;

        try {
            // write XMI
            out = new FileOutputStream(xmiFile);
            XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
            XMLSerializer xmlSer = new XMLSerializer(out, false);
            ser.serialize(aCas, xmlSer.getContentHandler());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Deserialize XMI into JCas
     *
     * @param jCas    The jCas to take the input.
     * @param xmiFile The input xmi file.
     * @throws IOException
     * @throws CollectionException
     */
    public static void readXmi(JCas jCas, File xmiFile) throws IOException, CollectionException {
        readXmi(jCas, xmiFile, false);
    }

    /**
     * Deserialize XMI into JCas
     *
     * @param jCas              The jCas to take the input.
     * @param xmiFile           The input xmi file.
     * @param failOnUnknownType Whether to fail on unknown types.
     * @throws IOException
     * @throws CollectionException
     */
    public static void readXmi(JCas jCas, File xmiFile, boolean failOnUnknownType) throws IOException,
            CollectionException {
        try (FileInputStream inputStream = new FileInputStream(xmiFile)) {
            XmiCasDeserializer.deserialize(inputStream, jCas.getCas(), !failOnUnknownType);
        } catch (SAXException e) {
            throw new CollectionException(e);
        }
    }

    /**
     * Retrieve the input file name from the source document information.
     *
     * @param srcDocInfoView   The view that contains the SourceDocumentInformation annotation.
     * @param outputFileSuffix The file suffix to output.
     * @return A filename that based on the input file. Null if input if cannot find input file name.
     * @throws AnalysisEngineProcessException
     */
    public static String getOutputFileName(JCas srcDocInfoView, String outputFileSuffix) throws
            AnalysisEngineProcessException {
        // Retrieve the filename of the input file from the CAS.
        try {
            SourceDocumentInformation fileLoc = JCasUtil.selectSingle(srcDocInfoView, SourceDocumentInformation.class);
            File inFile = new File(new URL(fileLoc.getUri()).getPath());
            StringBuilder buf = new StringBuilder();
            buf.append(inFile.getName());
            if (fileLoc.getOffsetInSource() > 0) {
                buf.append("_").append(fileLoc.getOffsetInSource());
            }
            buf.append(outputFileSuffix);
            return buf.toString();
        } catch (IllegalArgumentException | MalformedURLException e) {
            logger.info("Cannot find original input for file.");
            e.printStackTrace();
        }
        return null;
    }
}
