package com.o19s.solr;

import java.io.IOException;
import java.util.Set;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

/**
 * Some simple regex tests, mostly converted from contrib's TestRegexQuery.
 * The purpose is to test that a doc hits iff all of its terms are in the
 * regex. That is to say, if the field has a term, it must also be in the regex,
 * but if the regex has a term it doesn't necessarily have to be in the field.
 */
public class TestRegexpQuery extends SolrTestCaseJ4 {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initCore("solrconfig.xml", "schema.xml");
		createIndex();
	}

	public static void createIndex() {
		int i = 0;
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
			assertU(adoc(
					"id", Integer.toString(i),
					"sind", classif));
			i++;
		}
		assertU(commit());
	}

	/**
	 * Matches: All documents
	 * @throws IOException
	 */
	@Test
	public void testRegexStar() throws IOException {
		assertQ(req("{!rgx f=sind v=.*}"),
			"//*[@numFound='7']");
	}

	/** 
	 * Matches: Everything except "ORANGE"
	 * Misses: "ORANGE"
	 * @throws IOException
	 */
	@Test
	public void testCaveat() throws IOException {
		assertQ(req("{!rgx f=sind v=~(ORANGE)}"),
			"//*[@numFound='6']");
	}

	/** 
	 * Matches: "ORANGE"
	 * Misses: Anything that has "RED" or "YELLOW" in it
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void testCaveats() throws IOException {
		assertQ(req("{!rgx f=sind v=~(((,)?RED)?((,)?YELLOW)?)}"),
			"//*[@numFound='1']");
	}

	/** 
	 * Matches: "RED"
	 * Misses: "RED GREEN", "RED ORANGE"
	 * @throws IOException
	 */
	@Test
	public void testMatchSingleTerm() throws IOException {
		assertQuery(1, "RED");
	}

	/** 
	 * Matches: "ORANGE"
	 * Misses: "RED ORANGE"
	 * @throws IOException
	 */
	@Test
	public void testRegex2() throws IOException {
		assertQuery(1, "ORANGE");
	}

	/** 
	 * Matches: "YELLOW"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	@Test
	public void testRegex3() throws IOException {
		assertQuery(1, "YELLOW");
	}

	/** 
	 * Matches: Nothing
	 * Misses: "YELLOW BLUE"
	 * @throws IOException
	 */
	@Test
	public void testRegex5() throws IOException {
		assertQuery(0, makeRegexp("BLUE"));
	}

	/** 
	 * Matches: "RED", "ORANGE", "RED ORANGE"
	 * Misses: "RED GREEN"
	 * @throws IOException
	 */
	@Test
	public void testRegex6() throws IOException {
		assertQuery(3, makeRegexp("RED,ORANGE"));
	}

	/** 
	 * Matches: "YELLOW" (INDIGO is not indexed)
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	@Test
	public void testRegex7() throws IOException {
		assertQuery(1, makeRegexp("YELLOW,INDIGO"));
	}

	/** 
	 * Matches: "YELLOW", "ORANGE"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE", "RED ORANGE"
	 * @throws IOException
	 */
	@Test
	public void testRegex8() throws IOException {
		assertQuery(2, makeRegexp("YELLOW,ORANGE"));
	}

	/** 
	 * Matches: "YELLOW"
	 * Misses: "YELLOW GREEN", "YELLOW BLUE"
	 * @throws IOException
	 */
	@Test
	public void testRegex9() throws IOException {
		assertQuery(1, makeRegexp("YELLOW,INDIGO,VIOLET"));
	}

	/** 
	 * Matches: "YELLOW", "YELLOW BLUE"
	 * Misses: "YELLOW GREEN"
	 * @throws IOException
	 */
	@Test
	public void testComplexMultiple() throws IOException {
		assertQuery(2, makeRegexp("YELLOW,INDIGO,VIOLET,BLUE"));
	}

	/**
	 * Makes sure we're making the power set right. The number
	 * of sets returned should be 2^n, where n is the number
	 * of colors passed in.
	 */
	@Test
	@Ignore
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
	 * Stress test 2^14 queries or 16,384
	 */
	@Test
	@Ignore
	public void testPowerSets() {
		String colors = "RED,ORANGE,YELLOW,GREEN,BLUE,INDIGO,VIOLET," +
			"one,two,three,four,five,six,seven";
		Set<Set<String>> powerSet = makePowerSet(colors);
		StringBuffer colorBuffer = null;
		String colorSet = null;
		for (Set<String> set : powerSet) {
			colorBuffer = new StringBuffer();
			for (String color : set) {
				colorBuffer.append(color + ",");
			}
			if (colorBuffer.length() > 0) {
				colorSet = colorBuffer.toString();
				// delete the last comma
				colorSet.substring(0, colorSet.length());
				assertQ(req("{!rgx f=sind v="+colorSet+"}"),
					"//*");
			}
		}
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

	/**
	 * Helper for the assertQ method
	 * @param i Number of docs expected
	 * @param q The query
	 */
	private void assertQuery(int i, String q) {
		String query = "{!rgx f=sind v=" + q + "}";
		String found = "//*[@numFound='" + i + "']";
		assertQ(req(query), found);
	}
	
}
