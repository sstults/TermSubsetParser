package com.o19s.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTermSubsetQParserPlugin extends SolrTestCaseJ4 {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initCore("solrconfig.xml", "schema12.xml");
		createIndex();
	}

	public static void createIndex() {
		String v;
		v = "RED";
		assertU(adoc("id", "1", "shouldbeunstored", v, "text_np", v));
		v = "ORANGE";
		assertU(adoc("id", "2", "shouldbeunstored", v, "text_np", v));
		v = "YELLOW";
		assertU(adoc("id", "3", "shouldbeunstored", v, "text_np", v));
		v = "GREEN,YELLOW";
		assertU(adoc("id", "4", "shouldbeunstored", v, "text_np", v));
		v = "BLUE,YELLOW";
		assertU(adoc("id", "5", "shouldbeunstored", v, "text_np", v));
		v = "ORANGE,RED";
		assertU(adoc("id", "6", "shouldbeunstored", v, "text_np", v));
		v = "GREEN,RED";
		assertU(adoc("id", "7", "shouldbeunstored", v, "text_np", v));

		assertU(commit());
	}

	@Test
	public void testOneValue() {
		// should generate a phrase of "now cow" and match only one doc
		assertQ(req("{!termsubset f=shouldbeunstored v=RED}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=shouldbeunstored v=ORANGE}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=shouldbeunstored v=YELLOW}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=shouldbeunstored v=BLUE}"),
				"//*[@numFound='0']");
	}

	public void testTwoValues() {
		assertQ(req("{!termsubset f=shouldbeunstored v=ORANGE,RED}"),
				"//*[@numFound='3']");
		assertQ(req("{!termsubset f=shouldbeunstored v=INDIGO,YELLOW}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=shouldbeunstored v=ORANGE,YELLOW}"),
				"//*[@numFound='2']");
	}

	public void testThreeValues() {
		assertQ(req("{!termsubset f=shouldbeunstored v=INDIGO,VIOLET,YELLOW}"),
				"//*[@numFound='1']");
	}

	public void testFourValues() {
		assertQ(req("{!termsubset f=shouldbeunstored v=BLUE,INDIGO,VIOLET,YELLOW}"),
				"//*[@numFound='2']");
	}

	public void testFourReversedValues() {
		assertQ(req("{!termsubset f=shouldbeunstored v=YELLOW,VIOLET,INDIGO,BLUE}"),
				"//*[@numFound='2']");
	}

}
