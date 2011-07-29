package com.o19s.solr;

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

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Ignore;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

/**
 * Some simple regex tests, mostly converted from contrib's TestRegexQuery.
 * The purpose is to test that a doc hits iff all of its terms are in the
 * regex. That is to say, if the field has a term, it must also be in the regex.
 */
public class TestRegexpQuery extends LuceneTestCase {
	private IndexSearcher searcher;
	private IndexReader reader;
	private Directory directory;
	private final String FN = "field";

	@Override
	public void setUp() throws Exception {
		super.setUp();
		directory = newDirectory();
		RandomIndexWriter writer = new RandomIndexWriter(random, directory);
		String[] classifs = {
			"RED",
			"ORANGE",
			"YELLOW",
			"YELLOW,GREEN",
			"YELLOW,BLUE",
			"RED,ORANGE",
			"RED,GREEN"
		};
		for (String classif : classifs) {
			Document doc = new Document();
			doc.add(newField(FN, classif, Field.Store.NO, 
					Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}
		reader = writer.getReader();
		writer.commit();
		writer.close();
		searcher = newSearcher(reader);
	}

	@Override
	public void tearDown() throws Exception {
		searcher.close();
		reader.close();
		directory.close();
		super.tearDown();
	}

	/**
	 * Constructs a search term for the FN field
	 * @param value
	 * @return search term
	 */
	private Term newTerm(String value) {
		return new Term(FN, value);
	}

	/**
	 * Returns the number of query hits from the top five regex matches
	 * @param regex
	 * @return number of hits
	 * @throws IOException
	 */
	private int regexQueryNrHits(String regex) throws IOException {
		RegexpQuery query = new RegexpQuery(newTerm(regex));
		return searcher.search(query, 5).totalHits;
	}

	/**
	 * Matches: All documents
	 * @throws IOException
	 */
	public void testRegexStar() throws IOException {
		assertEquals(7, regexQueryNrHits(".*"));
	}

	/** 
	 * Matches: Everything except "ORANGE"
	 * Misses: "ORANGE"
	 * @throws IOException
	 */
	public void testCaveat() throws IOException {
		assertEquals(6, regexQueryNrHits("~(ORANGE)"));
	}

	/** 
	 * Matches: "ORANGE"
	 * Misses: Anything that has "RED" or "YELLOW" in it
	 * @throws IOException
	 */
	@Ignore
	public void testCaveats() throws IOException {
		assertEquals(1, regexQueryNrHits("~(((,)?RED)?((,)?YELLOW)?)"));
	}

	/** 
	 * Matches: "RED"
	 * Misses: "RED GREEN", "RED ORANGE"
	 * @throws IOException
	 */
	public void testMatchSingleTerm() throws IOException {
		assertEquals(1, regexQueryNrHits(makeRegexp("RED")));
	}

	/** 
	 * Matches: "ORANGE"
	 * Misses: "RED ORANGE"
	 * @throws IOException
	 */
	public void testRegex2() throws IOException {
		assertEquals(1, regexQueryNrHits(makeRegexp("ORANGE")));
	}

	/** 
	 * Matches: "YELLOW"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	public void testRegex3() throws IOException {
		assertEquals(1, regexQueryNrHits(makeRegexp("YELLOW")));
	}

	/** 
	 * Matches: Nothing
	 * Misses: "YELLOW BLUE"
	 * @throws IOException
	 */
	public void testRegex5() throws IOException {
		assertEquals(0, regexQueryNrHits(makeRegexp("BLUE")));
	}

	/** 
	 * Matches: "RED", "ORANGE", "RED ORANGE"
	 * Misses: "RED GREEN"
	 * @throws IOException
	 */
	public void testRegex6() throws IOException {
		assertEquals(3, regexQueryNrHits(makeRegexp("RED,ORANGE")));
	}

	/** 
	 * Matches: "YELLOW" (INDIGO is not indexed)
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	public void testRegex7() throws IOException {
		assertEquals(1, regexQueryNrHits(makeRegexp("YELLOW,INDIGO")));
	}

	/** 
	 * Matches: "YELLOW", "ORANGE"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE", "RED ORANGE"
	 * @throws IOException
	 */
	public void testRegex8() throws IOException {
		assertEquals(2, regexQueryNrHits(makeRegexp("YELLOW,ORANGE")));
	}

	/** 
	 * Matches: "YELLOW"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	public void testRegex9() throws IOException {
		assertEquals(1, regexQueryNrHits(makeRegexp("YELLOW,INDIGO,VIOLET")));
	}

	/** 
	 * Matches: "YELLOW", "YELLOW BLUE"
	 * Misses: "YELLOW GREEN"
	 * @throws IOException
	 */
	public void testComplexMultiple() throws IOException {
		assertEquals(2, regexQueryNrHits(makeRegexp("YELLOW,INDIGO,VIOLET,BLUE")));
	}

	/**
	 * Makes sure we're making the power set right. The number
	 * of sets returned should be 2^n, where n is the number
	 * of colors passed in.
	 */
	public void testMakePowerSet() {
		String colors = "RED";
		assertEquals((int)Math.pow(2, 1),makePowerSet(colors).size());
		colors = "RED,ORANGE";
		assertEquals((int)Math.pow(2, 2),makePowerSet(colors).size());
		colors = "RED,ORANGE,YELLOW";
		assertEquals((int)Math.pow(2, 3),makePowerSet(colors).size());
		colors = "RED,ORANGE,YELLOW,GREEN";
		assertEquals((int)Math.pow(2, 4),makePowerSet(colors).size());
		colors = "RED,ORANGE,YELLOW,GREEN,BLUE";
		assertEquals((int)Math.pow(2, 5),makePowerSet(colors).size());
		colors = "RED,ORANGE,YELLOW,GREEN,BLUE,INDIGO";
		assertEquals((int)Math.pow(2, 6),makePowerSet(colors).size());
		colors = "RED,ORANGE,YELLOW,GREEN,BLUE,INDIGO,VIOLET";
		assertEquals((int)Math.pow(2, 7),makePowerSet(colors).size());
	}
	
	/**
	 * Creates a set of optional space-separated strings in regexp form given a 
	 * comma-separated string of strings
	 * @param tags like "foo,bar,baz"
	 * @return a regexp like "(,)?(foo)?((,)?bar)?((,)?baz)?"
	 */
	private String makeRegexp(String tags) {
		StringBuffer regexpBuf = new StringBuffer();
		String[] tagA = tags.split(",");
		for (String tag : tagA) {
			regexpBuf.append("((,)?" + tag + ")?");
		}
		return regexpBuf.toString();
	}
	
	/**
	 * Constructs a power set of a given set
	 */
	private Set<Set<String>> makePowerSet(String tags) {
		return Sets.powerSet(ImmutableSortedSet.copyOf(tags.split(",")));
	}
}
