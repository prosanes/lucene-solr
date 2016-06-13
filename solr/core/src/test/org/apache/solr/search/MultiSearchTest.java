package org.apache.solr.search;

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

import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.response.transform.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;
import java.util.Random;

public class MultiSearchTest extends SolrTestCaseJ4 {

  @BeforeClass
  public static void beforeClass() throws Exception {
    System.setProperty("enable.update.log", "false"); // schema12 doesn't support _version_
    defaultHandler = "/multi";
    initCore("solrconfig.xml", "schema12.xml");
    String v = "how now brown cow";
    assertU(adoc("id","1", "text",v,  "text_np", v, "#foo_s", v));
    v = "now cow";
    assertU(adoc("id","2", "text",v,  "text_np", v));
    assertU(commit());
  }

  @Test
  public void testCopyRename() throws Exception {

    // original
    assertQ(req("count","2",
                "1.handler","standard",
                "1.q","id:1",
                "1.fl","id",
                "2.handler","standard",
                "2.q","id:2",
                "2.fl","id")
        ,"//result[@name='1.response']"
        ,"//result[@name='2.response']"
    );

  }
}
