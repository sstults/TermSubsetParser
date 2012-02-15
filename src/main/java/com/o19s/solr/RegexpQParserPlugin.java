package com.o19s.solr;

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
 * Simply constructs a RegexpQuery from the V parameter that operates over
 * the field named in F
 */
public class RegexpQParserPlugin extends QParserPlugin {
	public static String NAME = "rgx";
	public static final Logger log = LoggerFactory.getLogger(RegexpQParserPlugin.class);

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
				return new RegexpQuery(new Term(field, queryText));
			}
		};
	}

}
