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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This query parser takes a comma-delimited list of terms and constructs
 * a RegexpQuery. The field it queries is expected to be a comma-separated
 * list of terms in lexical order (the query terms need not be in order).
 * 
 * The result set of the query will be any doc who's full set of terms is a
 * subset of the query terms. That is to say, a query for "RED,BLUE" will not
 * match on a document who's field contents are "BLUE,GREEN,RED" but will match
 * on:
 * <ul>
 * <li>"RED"</li>
 * <li>"BLUE"</li>
 * <li>or "BLUE,RED"</li>
 * </ul>  
 */
public class TermSubsetQParserPlugin extends QParserPlugin {
	public static String NAME = "termsubset";
	public static final Logger log = LoggerFactory.getLogger(TermSubsetQParserPlugin.class);

	@SuppressWarnings("rawtypes")
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
