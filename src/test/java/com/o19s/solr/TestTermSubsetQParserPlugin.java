package com.o19s.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTermSubsetQParserPlugin extends SolrTestCaseJ4 {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initCore("solrconfig.xml", "schema.xml");
		createIndex();
	}

	public static void createIndex() {
		assertU(adoc("id", "1", "sind", "RED"));
		assertU(adoc("id", "2", "sind", "ORANGE"));
		assertU(adoc("id", "3", "sind", "YELLOW"));
		assertU(adoc("id", "4", "sind", "GREEN,YELLOW"));
		assertU(adoc("id", "5", "sind", "BLUE,YELLOW"));
		assertU(adoc("id", "6", "sind", "ORANGE,RED"));
		assertU(adoc("id", "7", "sind", "GREEN,RED"));
		assertU(commit());
	}

	@Test
	public void testOneValue() {
		assertQ(req("{!termsubset f=sind v=RED}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=sind v=ORANGE}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=sind v=YELLOW}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=sind v=BLUE}"),
				"//*[@numFound='0']");
	}

	public void testTwoValues() {
		assertQ(req("{!termsubset f=sind v=ORANGE,RED}"),
				"//*[@numFound='3']");
		assertQ(req("{!termsubset f=sind v=INDIGO,YELLOW}"),
				"//*[@numFound='1']");
		assertQ(req("{!termsubset f=sind v=ORANGE,YELLOW}"),
				"//*[@numFound='2']");
	}

	public void testThreeValues() {
		assertQ(req("{!termsubset f=sind v=INDIGO,VIOLET,YELLOW}"),
				"//*[@numFound='1']");
	}

	public void testFourValues() {
		assertQ(req("{!termsubset f=sind v=BLUE,INDIGO,VIOLET,YELLOW}"),
				"//*[@numFound='2']");
	}

	public void testFourReversedValues() {
		assertQ(req("{!termsubset f=sind v=YELLOW,VIOLET,INDIGO,BLUE}"),
				"//*[@numFound='2']");
	}

}
