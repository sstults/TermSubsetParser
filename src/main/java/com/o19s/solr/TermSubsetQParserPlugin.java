/**
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
package com.o19s.solr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;

/**
 * TODO: Re-do this documentation
 * 
 * Create a field query from the input value, applying text analysis and
 * constructing a phrase query if appropriate. <br>
 * Other parameters: <code>f</code>, the field <br>
 * Example: <code>{!field f=myfield}Foo Bar</code> creates a phrase query with
 * "foo" followed by "bar" if the analyzer for myfield is a text field with an
 * analyzer that splits on whitespace and lowercases terms. This is generally
 * equivalent to the Lucene query parser expression
 * <code>myfield:"Foo Bar"</code>
 */
public class TermSubsetQParserPlugin extends QParserPlugin {
	public static String NAME = "termsubset";

	public void init(NamedList args) {
	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		return new QParser(qstr, localParams, params, req) {
			@Override
			public Query parse() throws ParseException {
				String field = localParams.get(QueryParsing.F);
				String queryText = localParams.get(QueryParsing.V);

				return new RegexpQuery(new Term(field, makeRegexp(queryText)));
			}

			/**
			 * Creates a set of optional space-separated strings in regexp form
			 * given a comma-separated string of strings
			 * 
			 * @param tags
			 *            like "foo,bar,baz"
			 * @return a regexp like "((,)?foo)?((,)?bar)?((,)?baz)?"
			 */
			private String makeRegexp(String tags) {
				StringBuffer regexpBuf = new StringBuffer();
				String[] tagA = tags.split(",");
				List<String> list = Arrays.asList(tagA);
		        Collections.sort(list);
				for (String tag : list) {
					regexpBuf.append("((,)?" + tag + ")?");
				}
				return regexpBuf.toString();
			}
		};
	}

}
