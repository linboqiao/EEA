package edu.cmu.cs.lti.utils;

import org.apache.commons.io.IOUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/20/16
 * Time: 4:01 PM
 *
 * @author Zhengzhong Liu
 */
public class XMLUtils {
    public static String parseXMLTextWithOffsets(String xmlStr, boolean appendPeriod) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        StringBuilder strippedTextBuilder = new StringBuilder();

        XMLStreamReader streamReader = factory.createXMLStreamReader(IOUtils.toInputStream(xmlStr));

        while (streamReader.hasNext()) {
            streamReader.next();
            Location location = streamReader.getLocation();
            int startOffset = location.getCharacterOffset();

            if (streamReader.hasText()) {
                whiteSpacePad(strippedTextBuilder, startOffset);
                String text = streamReader.getText();
                strippedTextBuilder.append(text);

                String trimmedText = text.trim();

                // Append period to the end of a XML chunk.
                if (appendPeriod) {
                    if (!trimmedText.equals("") && !trimmedText.matches(".*\\p{Punct}$")) {
                        strippedTextBuilder.append(".");
                    }
                }
            }
        }

        return strippedTextBuilder.toString();
    }

    public static void whiteSpacePad(StringBuilder builder, int afterLength) {
        if (builder.length() >= afterLength) {
            return;
        }

        for (int i = builder.length(); i < afterLength - 1; i++) {
            builder.append(" ");
        }
        builder.append("\n");
    }
}
