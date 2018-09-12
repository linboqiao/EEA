package edu.cmu.cs.lti.script.solr;

import java.util.List;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrQueryEngine {
  public final SolrServer server;

  public SolrQueryEngine(String url) {
    server = new HttpSolrServer(url);
  }

  public void searchExample() throws SolrServerException {
    SolrQuery query = new SolrQuery();
    query.setQuery("bombing, bomb");
    query.setFields("id", "content", "title");
    query.setStart(0);
    query.setRows(100);
    QueryResponse response = server.query(query);

    System.out.println(response);

    SolrDocumentList results = response.getResults();
    for (int i = 0; i < results.size(); i++) {
      SolrDocument result = results.get(i);
      System.out.println(result.getFieldValue("id"));
      System.out.println(result.getFieldValue("content"));
    }
  }

  public void getTermResponse(String queryStr) throws SolrServerException {
    SolrQuery query = new SolrQuery(queryStr);
    query.setRequestHandler("/tvrh");
    query.setParam("tv", true);
    query.setParam("tv_df", true);
    QueryResponse queryResponse = server.query(query);
    TermsResponse termResponse = queryResponse.getTermsResponse();
    for (Entry<String, List<Term>> te : termResponse.getTermMap().entrySet()) {
      System.out.println(te.getKey());
    }

    // List<Term> terms = termResponse.getTerms("content");
    // for (Term term : terms) {
    // System.out.println("term " + term.getFrequency());
    // }
  }

  public static void main(String[] args) throws SolrServerException {
    SolrQueryEngine testEngine = new SolrQueryEngine(
            "http://cairo.lti.cs.edu.cmu.edu:8983/solr/gigaword");
    testEngine.getTermResponse("ipod");
    // testEngine.searchExample();
  }
}
