/**
 *
 */
package edu.cmu.cs.lti.ling;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zhengzhong Liu, Hector
 */
public class FrameDataReader {
    // The following methods wraps methods to read SemLink data from
    // http://verbs.colorado.edu/semlink/
    public static Map<Pair<String, String>, Pair<String, String>> getFN2VNRoleMap(String vfMappingPath, boolean inverse) {
        Map<Pair<String, String>, Pair<String, String>> fn2vn = new HashMap<>();

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            Document doc = builder.build(vfMappingPath);

            Element data = doc.getRootElement();

            List<Element> mappings = data.getChildren("vncls");

            for (Element mapping : mappings) {
                String frameName = mapping.getAttributeValue("fnframe");
                String vnClass = mapping.getAttributeValue("class");
                List<Element> roles = mapping.getChild("roles").getChildren("role");
                for (Element role : roles) {
                    String fnRole = role.getAttributeValue("fnrole");
                    String vnRole = role.getAttributeValue("vnrole");

                    Pair<String, String> framePair = new Pair<String, String>(frameName, fnRole);
                    Pair<String, String> vnPair = new Pair<String, String>(vnClass, vnRole);

                    if (inverse) {
                        fn2vn.put(vnPair, framePair);

                    } else {
                        fn2vn.put(framePair, vnPair);
                    }
                }
            }

        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }

//        System.out.println(String.format("%d mappings read from FrameNet to VerbNet", fn2vn.size()));

        return fn2vn;
    }

    public static Map<String, String> getFN2VNFrameMap(String vfMappingPath, boolean inverse) {

        Map<String, String> fn2vn = new HashMap<>();

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            Document doc = builder.build(vfMappingPath);

            Element data = doc.getRootElement();

            List<Element> mappings = data.getChildren("vncls");

            for (Element mapping : mappings) {
                String frameName = mapping.getAttributeValue("fnframe");
                String vnClass = mapping.getAttributeValue("class");
                List<Element> roles = mapping.getChild("roles").getChildren("role");
                if (inverse) {
                    fn2vn.put(vnClass, frameName);
                } else {
                    fn2vn.put(frameName, vnClass);
                }
            }

        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }

//        System.out.println(String.format("%d mappings read from FrameNet to VerbNet", fn2vn.size()));
        return fn2vn;
    }

    public static Map<Pair<String, String>, Pair<String, String>> getVN2PBRoleMap(String vpMappingPath, boolean inverse) {
        Map<Pair<String, String>, Pair<String, String>> vn2pb = new HashMap<>();

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);
            Document doc = builder.build(vpMappingPath);

            Element data = doc.getRootElement();

            List<Element> predicates = data.getChildren("predicate");

            for (Element predicate : predicates) {
                List<Element> mappings = predicate.getChildren("argmap");
                for (Element mapping : mappings) {
                    String vnClass = mapping.getAttributeValue("vn-class");
                    String pbRoleset = mapping.getAttributeValue("pb-roleset");

                    for (Element role : mapping.getChildren("roles")) {
                        String pbArg = role.getAttributeValue("pb-arg");
                        String vnRole = role.getAttributeValue("vn-theta");
                        if (inverse) {
                            vn2pb.put(new Pair<>(pbRoleset, pbArg), new Pair<>(vnClass, vnRole));
                        } else {
                            vn2pb.put(new Pair<>(vnClass, vnRole), new Pair<>(pbRoleset, pbArg));
                        }
                    }

                }
            }

        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }


        return vn2pb;

    }

    public static Map<String, String> getVN2PBFrameMap(String vpMappingPath, boolean inverse) {
        Map<String, String> vn2pb = new HashMap<>();

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);
            Document doc = builder.build(vpMappingPath);

            Element data = doc.getRootElement();

            List<Element> predicates = data.getChildren("predicate");

            for (Element predicate : predicates) {
                List<Element> mappings = predicate.getChildren("argmap");
                for (Element mapping : mappings) {
                    String vnClass = mapping.getAttributeValue("vn-class");
                    String pbRoleset = mapping.getAttributeValue("pb-roleset");

                    if (inverse) {
                        vn2pb.put(pbRoleset, vnClass);
                    } else {
                        vn2pb.put(vnClass, pbRoleset);
                    }
                }
            }

        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }

        return vn2pb;
    }

    // The following methods wraps methods to read data from FrameNet relations
    public static Map<String, Table<String, String, Map<String, String>>> getFrameRelations(
            String fnRelatonPath) {

        Map<String, Table<String, String, Map<String, String>>> frameRelationMappings = new HashMap<String, Table<String, String, Map<String, String>>>();
        int typeCounter = 0;
        int frameRelationCounter = 0;
        int feRelationCounter = 0;

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            Document doc = builder.build(fnRelatonPath);

            Element data = doc.getRootElement();

            Namespace ns = data.getNamespace();

            List<Element> relationTypeGroup = data.getChildren("frameRelationType", ns);

            for (Element relationsByType : relationTypeGroup) {
                typeCounter++;

                String relationType = relationsByType.getAttributeValue("name");

                Table<String, String, Map<String, String>> subFrame2SuperFrameMapping = HashBasedTable
                        .create();

                List<Element> frameRelations = relationsByType.getChildren("frameRelation", ns);
                for (Element frameRelation : frameRelations) {
                    frameRelationCounter++;

                    List<Element> feRelations = frameRelation.getChildren("FERelation", ns);

                    String subFrameName = frameRelation.getAttributeValue("subFrameName");
                    String superFrameName = frameRelation.getAttributeValue("superFrameName");

                    Map<String, String> feMapping = new HashMap<String, String>();

                    for (Element feRelation : feRelations) {
                        feRelationCounter++;

                        String subFeName = feRelation.getAttributeValue("subFEName");
                        String superFeName = feRelation.getAttributeValue("superFEName");
                        feMapping.put(subFeName, superFeName);
                    }

                    subFrame2SuperFrameMapping.put(subFrameName, superFrameName, feMapping);
                }

                frameRelationMappings.put(relationType, subFrame2SuperFrameMapping);
            }
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }

//        System.out.println(String.format(
//                "%d types, %s frame relations, %s frame element relations read", typeCounter,
//                frameRelationCounter, feRelationCounter));

        return frameRelationMappings;
    }

    public static String getFrameFromPropBankSense(String propBankSense, Map<String, String> pb2Vn, Map<String, String> vn2Fn) {
        if (propBankSense == null) {
            return null;
        }
        String vnFrame = pb2Vn.get(propBankSense);
        if (vnFrame != null) {
            return vn2Fn.get(vnFrame);
        }
        return null;
    }

    public static ArrayListMultimap<String, String> getFrame2Lexicon(String frameDirPath) throws IOException, ParserConfigurationException, SAXException {
        String[] frameDataExtensions = {"xml"};
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        ArrayListMultimap<String, String> frame2PostaggedLemma = ArrayListMultimap.create();

        for (File frameFile : FileUtils.listFiles(new File(frameDirPath), frameDataExtensions, true)) {
            org.w3c.dom.Document doc = builder.parse(frameFile);

            org.w3c.dom.Element root = doc.getDocumentElement();
            String frameName = root.getAttribute("name");

            NodeList lexUnits = doc.getElementsByTagName("lexUnit");
            for (int i = 0; i < lexUnits.getLength(); i++) {
                Node lexUnit = lexUnits.item(i);
                NodeList lexUnitContents = lexUnit.getChildNodes();
                for (int j = 0; j < lexUnitContents.getLength(); j++) {
                    Node lexUnitContent = lexUnitContents.item(j);
                    if (lexUnitContent.getNodeName().equals("lexeme")) {
                        NamedNodeMap lexeme = lexUnitContent.getAttributes();
                        String pos = lexeme.getNamedItem("POS").getNodeValue();
                        String lemma = lexeme.getNamedItem("name").getNodeValue();

                        frame2PostaggedLemma.put(frameName, lemma + "." + pos);
                    }
                }
            }
        }
        return frame2PostaggedLemma;
    }

    public static ArrayListMultimap<String, String> getLexicon2Frame(String frameDirPath) throws IOException, ParserConfigurationException, SAXException {
        String[] frameDataExtensions = {"xml"};
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        ArrayListMultimap<String, String> postaggedLemma2Frames = ArrayListMultimap.create();

        for (File frameFile : FileUtils.listFiles(new File(frameDirPath), frameDataExtensions, true)) {
            org.w3c.dom.Document doc = builder.parse(frameFile);

            org.w3c.dom.Element root = doc.getDocumentElement();
            String frameName = root.getAttribute("name");

            NodeList lexUnits = doc.getElementsByTagName("lexUnit");
            for (int i = 0; i < lexUnits.getLength(); i++) {
                Node lexUnit = lexUnits.item(i);
                NodeList lexUnitContents = lexUnit.getChildNodes();
                for (int j = 0; j < lexUnitContents.getLength(); j++) {
                    Node lexUnitContent = lexUnitContents.item(j);
                    if (lexUnitContent.getNodeName().equals("lexeme")) {
                        NamedNodeMap lexeme = lexUnitContent.getAttributes();
                        String pos = lexeme.getNamedItem("POS").getNodeValue();
                        String lemma = lexeme.getNamedItem("name").getNodeValue();
                        postaggedLemma2Frames.put(lemma + "." + pos, frameName);
                    }
                }
            }
        }

        return postaggedLemma2Frames;
    }


    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        ArrayListMultimap<String, String> frameLexicon =
                getFrame2Lexicon("/Users/zhengzhongliu/Documents/projects/cmu-script/data/resources/fndata-1.5/frame");
        for (Map.Entry<String, Collection<String>> frameLexiconEntry : frameLexicon.asMap().entrySet()) {
            System.out.println(String.format("Frame %s, lexical items: %s", frameLexiconEntry.getKey(), frameLexiconEntry.getValue()));
        }
    }

}
