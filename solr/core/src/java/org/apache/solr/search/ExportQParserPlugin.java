/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search;

import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.handler.component.MergeStrategy;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ExportQParserPlugin extends QParserPlugin {

  public static final String NAME = "xport";

  Logger logger = LoggerFactory.getLogger(ExportQParserPlugin.class);
  
  public void init(NamedList namedList) {
  }
  
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest request) {
    return new ExportQParser(qstr, localParams, params, request);
  }

  public class ExportQParser extends QParser {
    
    public ExportQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest request) {
      super(qstr, localParams, params, request);
    }
    
    public Query parse() throws SyntaxError {
      try {
          return new ExportQuery(localParams, params, req);
        } catch (Exception e) {
          throw new SyntaxError(e.getMessage(), e);
        }
    }
  }

  public class ExportQuery extends RankQuery {
    
    private Query mainQuery;
    private Object id;

    public RankQuery clone() {
      ExportQuery clone = new ExportQuery();
      clone.id = id;
      return clone;
    }

    public RankQuery wrap(Query mainQuery) {
      this.mainQuery = mainQuery;
      return this;
    }

    public MergeStrategy getMergeStrategy() {
      return null;
    }

    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException{
      return mainQuery.createWeight(searcher, true);
    }

    public Query rewrite(IndexReader reader) throws IOException {
      Query q = mainQuery.rewrite(reader);
      if(q == mainQuery) {
        return super.rewrite(reader);
      } else {
        return clone().wrap(q);
      }
    }

    public TopDocsCollector getTopDocsCollector(int len,
                                                SolrIndexSearcher.QueryCommand cmd,
                                                IndexSearcher searcher) throws IOException {
      int leafCount = searcher.getTopReaderContext().leaves().size();
      FixedBitSet[] sets = new FixedBitSet[leafCount];
      return new ExportCollector(sets);
    }

    public int hashCode() {
      return 31 * super.hashCode() + id.hashCode();
    }
    
    public boolean equals(Object o) {
      if (super.equals(o) == false) {
        return false;
      }
      ExportQuery q = (ExportQuery)o;
      return id == q.id;
    }
    
    public String toString(String s) {
      return s;
    }

    public ExportQuery() {

    }
    
    public ExportQuery(SolrParams localParams, SolrParams params, SolrQueryRequest request) throws IOException {
      id = new Object();
    }
  }
  
  private class ExportCollector extends TopDocsCollector  {

    private FixedBitSet[] sets;

    public ExportCollector(FixedBitSet[] sets) {
      super(null);
      this.sets = sets;
    }

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
      final FixedBitSet set = new FixedBitSet(context.reader().maxDoc());
      this.sets[context.ord] = set;
      return new LeafCollector() {
        
        @Override
        public void setScorer(Scorer scorer) throws IOException {}
        
        @Override
        public void collect(int docId) throws IOException{
          ++totalHits;
          set.set(docId);
        }
      };
    }

    private ScoreDoc[] getScoreDocs(int howMany) {
      ScoreDoc[] docs = new ScoreDoc[Math.min(totalHits, howMany)];
      for(int i=0; i<docs.length; i++) {
        docs[i] = new ScoreDoc(i,0);
      }

      return docs;
    }

    public TopDocs topDocs(int start, int howMany) {

      assert(sets != null);

      SolrRequestInfo info = SolrRequestInfo.getRequestInfo();

      SolrQueryRequest req = null;
      if(info != null && ((req = info.getReq()) != null)) {
        Map context = req.getContext();
        context.put("export", sets);
        context.put("totalHits", totalHits);
      }

      ScoreDoc[] scoreDocs = getScoreDocs(howMany);
      assert scoreDocs.length <= totalHits;
      return new TopDocs(totalHits, scoreDocs, 0.0f);
    }

    @Override
    public boolean needsScores() {
      return true; // TODO: is this the case?
    }
  }

}