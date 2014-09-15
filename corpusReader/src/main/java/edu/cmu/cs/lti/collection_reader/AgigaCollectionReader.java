/**
 * 
 */
package edu.cmu.cs.lti.collection_reader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.cmu.cs.lti.uima.io.reader.AbstractStepBasedDirReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.jhu.agiga.AgigaCoref;
import edu.jhu.agiga.AgigaDocument;
import edu.jhu.agiga.AgigaMention;
import edu.jhu.agiga.AgigaPrefs;
import edu.jhu.agiga.AgigaSentence;
import edu.jhu.agiga.AgigaToken;
import edu.jhu.agiga.AgigaTypedDependency;
import edu.jhu.agiga.StreamingDocumentReader;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EndIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author zhengzhongliu
 * 
 */
public class AgigaCollectionReader extends CollectionReader_ImplBase {

  public static final String PARAM_INPUTDIR = "InputDirectory";

  private File[] gzFileList;

  private int gCurrentIndex;

  private int fileOffset;

  private StreamingDocumentReader reader;

  private AgigaPrefs prefs = new AgigaPrefs();

  public static final String COMPONENT_ID = AgigaCollectionReader.class.getSimpleName();

  @Override
  public void initialize() {
    File inputDir = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());

    gzFileList = inputDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".gz");
      }
    });

    if (gzFileList.length > 0) {
      reader = new StreamingDocumentReader(gzFileList[0].getAbsolutePath(), prefs);
    }
  }

  @Override
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new CollectionException(e);
    }

    if (!reader.hasNext()) {
      reader = new StreamingDocumentReader(gzFileList[gCurrentIndex++].getAbsolutePath(), prefs);
      fileOffset = 0;
    }

    AgigaDocument currentDoc = reader.next();
    fileOffset += 1;
    uimafyAnnotations(jcas, currentDoc);

    SourceDocumentInformation enSrcDocInfo = new SourceDocumentInformation(jcas);
    enSrcDocInfo.setUri(gzFileList[0].toURI().toURL().toString());
    enSrcDocInfo.setOffsetInSource(fileOffset);
    enSrcDocInfo.setDocumentSize((int) gzFileList[0].length());
    enSrcDocInfo.setLastSegment(!reader.hasNext());
    enSrcDocInfo.addToIndexes();
  }

  private void uimafyAnnotations(JCas jcas, AgigaDocument doc) {
    StringBuilder builder = new StringBuilder();

    int sentBegin = 0;
    int sentEnd = 0;

    int tokenBegin = 0;
    int tokenEnd = 0;

    int offset = 0;

    int tokenIdx = 0;
    int sentIdx = 0;

    Table<StanfordCorenlpToken, StanfordCorenlpToken, EntityMention> index2Mentions = HashBasedTable
            .create();

    List<EntityMention> allMentions = new ArrayList<EntityMention>();

    List<List<StanfordCorenlpToken>> allTokens = new ArrayList<List<StanfordCorenlpToken>>();

    for (AgigaSentence aSent : doc.getSents()) {
      sentBegin = offset;

      List<StanfordCorenlpToken> sTokens = new ArrayList<StanfordCorenlpToken>();

      String lastNETag = "O";
      StanfordCorenlpToken beginToken = null;
      StanfordCorenlpToken endToken = null;

      for (AgigaToken aToken : aSent.getTokens()) {
        tokenBegin = offset;
        String tokenSurface = aToken.getWord();
        builder.append(tokenSurface);
        builder.append(" ");
        offset += tokenSurface.length();
        tokenEnd = offset;
        offset++;

        StanfordCorenlpToken sToken = new StanfordCorenlpToken(jcas);
        UimaAnnotationUtils
                .finishAnnotation(sToken, tokenBegin, tokenEnd, COMPONENT_ID, tokenIdx++);
        sToken.setPos(aToken.getPosTag());
        sToken.setLemma(aToken.getLemma());

        sTokens.add(sToken);

        // add named entities
        String neTag = aToken.getNerTag();
        sToken.setNerTag(aToken.getNerTag());

        if (neTag.equals("O") && !lastNETag.equals("O")) {
          // ne == O, lastNE = E
          EntityMention mention = new EntityMention(jcas, beginToken.getBegin(), endToken.getEnd());
          mention.setEntityType(lastNETag);
          allMentions.add(mention);
          index2Mentions.put(beginToken, endToken, mention);
          // System.out.println("Meet O, Creating "+mention.getCoveredText()+" "+beginToken+" "+endToken+
          // " "+lastNETag);
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
            // System.out.println("Meeting another, Creating "+mention.getCoveredText()+" "+beginToken+" "+endToken+" "+lastNETag);
          }
          endToken = sToken;
        }
        lastNETag = neTag;
      }
      if (!lastNETag.equals("O")) {
        EntityMention mention = new EntityMention(jcas, beginToken.getBegin(), endToken.getEnd());
        mention.setEntityType(lastNETag);
        allMentions.add(mention);
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
      UimaAnnotationUtils.finishAnnotation(root, COMPONENT_ID, sentIdx);
      root.setIsRoot(true);

      this.addTreebankNodeToIndexes(root, jcas, tree, sTokens);

      ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> headRelations = ArrayListMultimap
              .create();
      ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> childRelations = ArrayListMultimap
              .create();

      // add dependencies
      for (AgigaTypedDependency dep : aSent.getColCcprocDeps()) {
        StanfordCorenlpToken childToken = sTokens.get(dep.getDepIdx());
        int headIndex = dep.getGovIdx();
        if (headIndex == -1) {
          childToken.setIsDependencyRoot(true);
        } else {
          StanfordCorenlpToken headToken = sTokens.get(headIndex);

          StanfordDependencyRelation relation = new StanfordDependencyRelation(jcas);
          UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, null);
          relation.setHead(headToken);
          relation.setChild(childToken);

          headRelations.put(childToken, relation);
          childRelations.put(headToken, relation);
        }
      }

      for (StanfordCorenlpToken token : headRelations.keys()) {
        token.setChildDependencyRelations(FSCollectionFactory.createFSList(jcas,
                headRelations.get(token)));
        token.setHeadDependencyRelations(FSCollectionFactory.createFSList(jcas,
                childRelations.get(token)));
      }

      StanfordCorenlpSentence sSent = new StanfordCorenlpSentence(jcas);
      UimaAnnotationUtils.finishAnnotation(sSent, sentBegin, sentEnd, COMPONENT_ID, sentIdx++);

      allTokens.add(sTokens);
    }
    jcas.setDocumentText(builder.toString());

    List<Entity> entities = new ArrayList<Entity>();

    // coreference
    for (AgigaCoref coref : doc.getCorefs()) {
      List<EntityMention> corefMentions = new ArrayList<EntityMention>();
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
      UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionIdx++);
      if (mention.getReferingEntity() == null) {
        Entity entity = new Entity(jcas);
        entity.setEntityMentions(new FSArray(jcas, 1));
        entity.setEntityMentions(0, mention);
        mention.setReferingEntity(entity);
        mention.setHead(UimaAnnotationUtils.findHeadFromTreeAnnotation(jcas, mention));
        entities.add(entity);
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
      UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, entityIdx++);
      entity.addToIndexes();
    }

    Article article = new Article(jcas);
    UimaAnnotationUtils.finishAnnotation(article, 0, offset, COMPONENT_ID, null);
    article.setArticleName(doc.getDocId());
    article.setLanguage("en");
  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(gCurrentIndex, gzFileList.length, Progress.ENTITIES) };
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {

    if (reader.hasNext()) {
      return true;
    } else if (gCurrentIndex < gzFileList.length) {
      reader = new StreamingDocumentReader(gzFileList[gCurrentIndex++].getAbsolutePath(), prefs);
      if (reader.hasNext()) {
        return true;
      }
    }

    return false;
  }

  private FSArray addTreebankNodeChildrenToIndexes(StanfordTreeAnnotation parent, JCas jCas,
          List<StanfordCorenlpToken> tokens, Tree tree) {
    Tree[] childTrees = tree.children();

    // collect all children (except leaves, which are just the words - POS tags are pre-terminals in
    // a Stanford tree)
    List<StanfordTreeAnnotation> childNodes = new ArrayList<StanfordTreeAnnotation>();
    for (Tree child : childTrees) {
      if (!child.isLeaf()) {
        // set node attributes and add children (mutual recursion)
        StanfordTreeAnnotation node = new StanfordTreeAnnotation(jCas);
        node.setParent(parent);
        this.addTreebankNodeToIndexes(node, jCas, child, tokens);
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
          List<StanfordCorenlpToken> tokens) {
    // figure out begin and end character offsets
    CoreMap label = (CoreMap) tree.label();
    StanfordCorenlpToken beginToken = tokens.get(label.get(BeginIndexAnnotation.class));
    StanfordCorenlpToken endToken = tokens.get(label.get(EndIndexAnnotation.class) - 1);

    int nodeBegin = beginToken.getBegin();
    int nodeEnd = endToken.getEnd();

    // set span, node type, children (mutual recursion), and add it to the JCas
    UimaAnnotationUtils.finishAnnotation(node, nodeBegin, nodeEnd, COMPONENT_ID, null);
    node.setPennTreeLabel(tree.value());
    node.setChildren(this.addTreebankNodeChildrenToIndexes(node, jCas, tokens, tree));
    node.setIsLeaf(node.getChildren().size() == 0);
    node.addToIndexes();
  }

}
