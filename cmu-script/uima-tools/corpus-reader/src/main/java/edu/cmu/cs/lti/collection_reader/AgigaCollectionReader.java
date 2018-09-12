/**
 *
 */
package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.jhu.agiga.*;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//import edu.stanford.nlp.ling.Word;

//import edu.stanford.nlp.ling.Word;

/**
 * @author zhengzhongliu
 */
public class AgigaCollectionReader extends AbstractCollectionReader {

    public static final String PARAM_INPUTDIR = "InputDirectory";

    private File[] gzFileList;

    private int gCurrentIndex;

    private File currentFile;

    private int fileOffset;

    private StreamingDocumentReader reader;

    private AgigaPrefs prefs = new AgigaPrefs();

    public static final String COMPONENT_ID = AgigaCollectionReader.class.getSimpleName();

    private HeadFinder shf = new SemanticHeadFinder();

    int treeId = 0;

    private static String className = AgigaCollectionReader.class.getSimpleName();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        File inputDir = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
        gCurrentIndex = 0;

        if (!(inputDir.exists() && inputDir.isDirectory())) {
            throw new ResourceInitializationException(new FileNotFoundException(inputDir.getAbsolutePath()));
        }

        gzFileList = inputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".gz");
            }
        });

        if (gzFileList.length > 0) {
            readNewGzFile();
        }
    }

    @Override
    public void getNext(JCas jcas) throws IOException, CollectionException {
        if (!reader.hasNext()) {
            readNewGzFile();
        }

        AgigaDocument currentDoc = reader.next();
        fileOffset += 1;
        treeId = 0;
        uimafyAnnotations(jcas, currentDoc);
        UimaAnnotationUtils.setSourceDocumentInformation(jcas, currentFile.toURI().toURL().toString(), (int) currentFile.length(), fileOffset, !reader.hasNext());

    }


    @Override
    public boolean hasNext() throws IOException, CollectionException {
        if (reader.hasNext()) {
            return true;
        } else if (gCurrentIndex < gzFileList.length) {
            readNewGzFile();
            if (reader.hasNext()) {
                return true;
            }
        }

        return false;
    }

    private void readNewGzFile() {
        currentFile = gzFileList[gCurrentIndex];
        reader = new StreamingDocumentReader(gzFileList[gCurrentIndex++].getAbsolutePath(), prefs);
        fileOffset = 0;
    }

    private void uimafyAnnotations(JCas jcas, AgigaDocument doc) {
        StringBuilder builder = new StringBuilder();

        int offset = 0;

        Table<StanfordCorenlpToken, StanfordCorenlpToken, EntityMention> index2Mentions = HashBasedTable
                .create();

        List<EntityMention> allMentions = new ArrayList<EntityMention>();

        List<List<StanfordCorenlpToken>> allTokens = new ArrayList<List<StanfordCorenlpToken>>();

        int sentIdx = 0;
        int tokenIdx = 0;

        for (AgigaSentence aSent : doc.getSents()) {
            int sentBegin = 0;
            int sentEnd = 0;

            sentBegin = offset;

            List<StanfordCorenlpToken> sTokens = new ArrayList<StanfordCorenlpToken>();

            String lastNETag = "O";
            StanfordCorenlpToken beginToken = null;
            StanfordCorenlpToken endToken = null;

            for (AgigaToken aToken : aSent.getTokens()) {
                int tokenBegin = offset;

                tokenBegin = offset;
                String tokenSurface = aToken.getWord();
                builder.append(tokenSurface);
                builder.append(" ");
                offset += tokenSurface.length();
                int tokenEnd = offset;
                offset++;

                StanfordCorenlpToken sToken = new StanfordCorenlpToken(jcas, tokenBegin, tokenEnd);
                UimaAnnotationUtils.finishAnnotation(sToken, COMPONENT_ID, tokenIdx++, jcas);
                sToken.setPos(aToken.getPosTag());
                sToken.setLemma(aToken.getLemma());
                // add named entities
                String neTag = aToken.getNerTag();
                if (neTag == null) {
                    neTag = "O";
                }
                sToken.setNerTag(aToken.getNerTag());
                sTokens.add(sToken);

                if (neTag.equals("O") && !lastNETag.equals("O")) {
                    // ne == O, lastNE = E
                    EntityMention mention = new EntityMention(jcas, beginToken.getBegin(), endToken.getEnd());
                    mention.setEntityType(lastNETag);
                    allMentions.add(mention);
                    index2Mentions.put(beginToken, endToken, mention);
                } else {
                    if (lastNETag.equals("O")) {
                        beginToken = sToken;
                    } else if (lastNETag.equals(neTag)) {
                    } else {
                        EntityMention mention = new EntityMention(jcas, beginToken.getBegin(),
                                endToken.getEnd());
                        mention.setEntityType(lastNETag);
                        allMentions.add(mention);
                        beginToken = sToken;
                        index2Mentions.put(beginToken, endToken, mention);
                    }
                    endToken = sToken;
                }
                lastNETag = neTag;
            }

            if (!lastNETag.equals("O")) {
                EntityMention mention = new EntityMention(jcas, beginToken.getBegin(), endToken.getEnd());
                mention.setEntityType(lastNETag);
                allMentions.add(mention);
                index2Mentions.put(beginToken, endToken, mention);
            }

            sentEnd = offset;
            builder.append("\n");
            offset += 1;

            // add trees
            Tree tree = aSent.getStanfordContituencyTree();
            if (tree.children().length != 1) {
                throw new RuntimeException("Expected single root node, found " + tree);
            }
            tree = tree.firstChild();
            tree.indexSpans(0);
            StanfordTreeAnnotation root = new StanfordTreeAnnotation(jcas);
            root.setIsRoot(true);
            this.addTreebankNodeToIndexes(root, jcas, tree, sTokens, sentBegin);

            ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> headRelations = ArrayListMultimap
                    .create();
            ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> childRelations = ArrayListMultimap
                    .create();

            // add dependencies
            for (AgigaTypedDependency dep : aSent.getColCcprocDeps()) {
                StanfordCorenlpToken childToken = sTokens.get(dep.getDepIdx());
                int headIndex = dep.getGovIdx();

                String depType = dep.getType();

                if (depType.equals("root")) {
                    childToken.setIsDependencyRoot(true);
                }

                if (headIndex != -1) {
                    StanfordCorenlpToken headToken = sTokens.get(headIndex);

                    StanfordDependencyRelation relation = new StanfordDependencyRelation(jcas);
                    UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, null, jcas);
                    relation.setHead(headToken);
                    relation.setChild(childToken);
                    relation.setDependencyType(depType);

                    headRelations.put(childToken, relation);
                    childRelations.put(headToken, relation);
                } else {
//                    System.out.println("Head is -1, root is "+ dep.getDepIdx() + " "+dep.getGovIdx());
                }
            }

            for (StanfordCorenlpToken token : headRelations.keys()) {
                token.setHeadDependencyRelations(FSCollectionFactory.createFSList(jcas,
                        headRelations.get(token)));
            }

            for (StanfordCorenlpToken token : childRelations.keys()) {
                token.setChildDependencyRelations(FSCollectionFactory.createFSList(jcas,
                        childRelations.get(token)));
            }

            StanfordCorenlpSentence sSent = new StanfordCorenlpSentence(jcas);
            UimaAnnotationUtils
                    .finishAnnotation(sSent, sentBegin, sentEnd, COMPONENT_ID, sentIdx++, jcas);

            allTokens.add(sTokens);
        }
        jcas.setDocumentText(builder.toString());

        List<Entity> entities = new ArrayList<Entity>();

        // coreference
        for (AgigaCoref coref : doc.getCorefs()) {
            List<EntityMention> corefMentions = new ArrayList<EntityMention>();
            EntityMention representativeMention = null;
            for (AgigaMention aMention : coref.getMentions()) {
                int aStartTokenIdx = aMention.getStartTokenIdx();
                int aEndTokenIdx = aMention.getEndTokenIdx();
                int aSentIdx = aMention.getSentenceIdx();
                int aHeadTokenIdx = aMention.getHeadTokenIdx();
                aMention.getSentenceIdx();
                StanfordCorenlpToken sStartToken = allTokens.get(aSentIdx).get(aStartTokenIdx);
                StanfordCorenlpToken sEndToken = allTokens.get(aSentIdx).get(aEndTokenIdx - 1);
                StanfordCorenlpToken sHeadToken = allTokens.get(aSentIdx).get(aHeadTokenIdx);
                EntityMention mention = index2Mentions.get(sStartToken, sEndToken);
                if (mention == null) {
                    mention = new EntityMention(jcas, sStartToken.getBegin(), sEndToken.getEnd());
                    allMentions.add(mention);
                }
                corefMentions.add(mention);
                if (aMention.isRepresentative()) {
                    representativeMention = mention;
                }
            }

            // create an entity for the mentions
            Collections.sort(corefMentions, new Comparator<EntityMention>() {
                @Override
                public int compare(EntityMention m1, EntityMention m2) {
                    return m1.getBegin() - m2.getBegin();
                }
            });

            Entity entity = new Entity(jcas);
            entity.setEntityMentions(new FSArray(jcas, corefMentions.size()));
            entity.setRepresentativeMention(representativeMention);
//            System.out.println(entity.getRepresentativeMention().getCoveredText());
            int index = 0;
            for (EntityMention mention : corefMentions) {
                mention.setReferingEntity(entity);
                entity.setEntityMentions(index, mention);
                index += 1;
            }
            entities.add(entity);
        }

        Collections.sort(allMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention m1, EntityMention m2) {
                return m1.getBegin() - m2.getBegin();
            }
        });

        int mentionIdx = 0;
        for (EntityMention mention : allMentions) {
            UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionIdx++, jcas);
            if (mention.getReferingEntity() == null) {
                Entity entity = new Entity(jcas);
                entity.setEntityMentions(new FSArray(jcas, 1));
                entity.setEntityMentions(0, mention);
                mention.setReferingEntity(entity);
                entity.setRepresentativeMention(mention);
//                System.out.println(entity.getRepresentativeMention().getCoveredText());
                entities.add(entity);
            }

            if (mention.getHead() == null) {
                mention.setHead(UimaNlpUtils.findHeadFromAnnotation(mention));
            }
        }

        // sort entities by document order
        Collections.sort(entities, new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return getFirstBegin(o1) - getFirstBegin(o2);
            }

            private int getFirstBegin(Entity entity) {
                int min = Integer.MAX_VALUE;
                for (EntityMention mention : JCasUtil.select(entity.getEntityMentions(),
                        EntityMention.class)) {
                    if (mention.getBegin() < min) {
                        min = mention.getBegin();
                    }
                }
                return min;
            }
        });

        // add entities to document
        int entityIdx = 0;
        for (Entity entity : entities) {
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, entityIdx++, jcas);
        }

        Article article = new Article(jcas);
        UimaAnnotationUtils.finishAnnotation(article, 0, offset, COMPONENT_ID, null, jcas);
        article.setArticleName(doc.getDocId());
        article.setLanguage("en");
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(gCurrentIndex, gzFileList.length, Progress.ENTITIES)};
    }


    private FSArray addTreebankNodeChildrenToIndexes(StanfordTreeAnnotation parent, JCas jCas,
                                                     List<StanfordCorenlpToken> tokens, Tree tree, int offset) {
        Tree[] childTrees = tree.children();

        // collect all children (except leaves, which are just the words - POS tags are pre-terminals in
        // a Stanford tree)
        List<StanfordTreeAnnotation> childNodes = new ArrayList<StanfordTreeAnnotation>();
        for (Tree child : childTrees) {
            if (!child.isLeaf()) {
                // set node attributes and add children (mutual recursion)
                StanfordTreeAnnotation node = new StanfordTreeAnnotation(jCas);
                node.setParent(parent);
                this.addTreebankNodeToIndexes(node, jCas, child, tokens, offset);
                childNodes.add(node);
            }
        }

        // convert the child list into an FSArray
        FSArray childNodeArray = new FSArray(jCas, childNodes.size());
        for (int i = 0; i < childNodes.size(); ++i) {
            childNodeArray.set(i, childNodes.get(i));
        }
        return childNodeArray;
    }

    private void addTreebankNodeToIndexes(StanfordTreeAnnotation node, JCas jCas, Tree tree,
                                          List<StanfordCorenlpToken> tokens, int textOffset) {
        // figure out begin and end character offsets
        CoreMap label = (CoreMap) tree.label();
        StanfordCorenlpToken beginToken = tokens.get(label.get(BeginIndexAnnotation.class));
        StanfordCorenlpToken endToken = tokens.get(label.get(EndIndexAnnotation.class) - 1);

        int nodeBegin = beginToken.getBegin();
        int nodeEnd = endToken.getEnd();

        // set span, node type, children (mutual recursion), and add it to the JCas

        node.setPennTreeLabel(tree.value());
        node.setChildren(this.addTreebankNodeChildrenToIndexes(node, jCas, tokens, tree, textOffset));

        UimaAnnotationUtils.finishAnnotation(node, nodeBegin, nodeEnd, COMPONENT_ID, treeId++, jCas);

        boolean isLeaf = node.getChildren().size() == 0;
        node.setIsLeaf(isLeaf);

        List<StanfordCorenlpToken> leafTokens = JCasUtil
                .selectCovered(StanfordCorenlpToken.class, node);

        if (leafTokens.size() == 0) {
            return;
        }

        int leaveIndex = 0;
        if (!isLeaf) {
            Tree headLeaf = tree.headTerminal(shf);
            for (Tree leaf : tree.getLeaves()) {
                if (leaf.equals(headLeaf)) {
                    break;
                }
                leaveIndex++;
            }
        }

        StanfordCorenlpToken leafToken = leafTokens.get(leaveIndex);
        node.setHead(leafToken);
    }


    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        String paramInputDir = args[0];// "/Users/zhengzhongliu/Documents/data/agiga_sample";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "xmi";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AgigaCollectionReader.class, typeSystemDescription,
                AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                paramOutputFileSuffix);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }
}
