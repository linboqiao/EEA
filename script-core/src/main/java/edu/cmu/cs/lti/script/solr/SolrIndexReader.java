package edu.cmu.cs.lti.script.solr;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/26/14
 * Time: 11:33 PM
 */

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;

/**
 * Just to demonstrate how to get a term's document frequency
 */
public class SolrIndexReader {
    private IndexReader indexReader;

    private Directory dirIndex;

    public SolrIndexReader(String indexPath) throws IOException {
        dirIndex = FSDirectory.open(new File(indexPath));
        indexReader = IndexReader.open(dirIndex);
    }

    public int getDocumentFrequency(String term) throws IOException {
        return getDocumentFrequency("text", term);
    }

    public int getDocumentFrequency(String field, String term) throws IOException {
        Term sampleTerm = new Term(field, term);
        return indexReader.docFreq(sampleTerm);
    }

    protected void finalize() throws IOException {
        indexReader.close();
        dirIndex.close();
    }

    public static void main(String[] args) throws IOException {

        SolrIndexReader reader = new SolrIndexReader(
                "/Users/zhengzhongliu/tools/solr-4.7.0/example/solr/collection1/data/index");

        System.out.println(reader.getDocumentFrequency("he"));
    }
}
