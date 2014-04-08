package org.kang.lucene.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.UpperCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestIndexerAndSearcher {

	private Searcher searcher;
	private Query query;
	private TopDocs hits;

	@Before
	public void setUp() {
		Indexer indexer = CoreFactory.newDefaultIndexer();

		indexer.deleteAll();

		// @formatter:off
		indexer.prepareDocument()
		.addField(new TextField("head", "title title1", Store.YES))
		.addField(new TextField("body", "body body1", Store.YES))
		.addField(new TextField("keyword", "keyword1", Store.NO))
		.addField(new TextField("keyword", "keyword3", Store.NO))
		.addDocument();

		indexer.prepareDocument()
		.addField(new TextField("head", "title title2", Store.YES))
		.addField(new TextField("body", "body body2", Store.YES))
		.addField(new TextField("keyword", "keyword2", Store.NO))
		.addField(new TextField("keyword", "keyword3", Store.NO))
		.addDocument();
		// @formatter:on

		indexer.close();

		searcher = CoreFactory.newDefaultSearcher();
	}

	@After
	public void tearDown() {
		searcher.close();
	}

	@Test
	public void testSearchingWithTermQuery() throws Exception {
		query = new TermQuery(new Term("head", "title"));

		hits = searcher.search(query, 10);

		assertTotalHitCount(2);
		assertField("title title1", 0, "head");
		assertField("title title2", 1, "head");
	}

	@Test
	public void testSearchingWithQueryParser() throws Exception {
		QueryParser parser = CoreFactory.newDefaultMultiFieldQueryParser(
				"head", "body", "keyword");

		query = parser.parse("keyword3");
		hits = searcher.search(query, 10);
		assertTotalHitCount(2);
		assertField("title title1", 0, "head");
		assertField("title title2", 1, "head");

		query = parser.parse("title AND title1");
		hits = searcher.search(query, 10);
		assertTotalHitCount(1);
		assertField("title title1", 0, "head");
	}

	private void endOfRead(CharBuffer cbuff) {
		if (cbuff.remaining() > 0) {
			cbuff.compact();
		} else {
			cbuff.clear();
		}
	}

	// TODO: introduce strategy patterns.
	private boolean isBlackWord(char c) {
		return c == 'B';
	}

	private boolean isWhiteWord(char c) {
		return Character.isLetterOrDigit(c);
	}

	// TODO: handle character types. ex) 'a3' => 'a', '3'
	// it should use look ahead 1 character.
	private void getToken(Reader input, CharBuffer cbuff, StringBuffer strbuff)
			throws Exception {
		strbuff.delete(0, strbuff.length());

		int res = input.read(cbuff);
		if (res == -1) {
			return;
		}
		cbuff.flip();

		while (true) {
			if (cbuff.remaining() == 0) {
				cbuff.clear();
				res = input.read(cbuff);
				if (res == -1) {
					return;
				}
				cbuff.flip();
			}

			char c = cbuff.get();
			if (c == -1) {
				endOfRead(cbuff);
				return;
			} else if (isBlackWord(c)) {
				if (strbuff.length() == 0) {
					// skip
				} else {
					endOfRead(cbuff);
					return;
				}
			} else if (isWhiteWord(c)) {
				strbuff.append(c);
			} else { // handles like black word.
				if (strbuff.length() == 0) {
					// skip
				} else {
					endOfRead(cbuff);
					return;
				}
			}
		}
	}

	class CustomCharTokenizer1 extends CharTokenizer {

		public CustomCharTokenizer1(Version matchVersion, Reader input) {
			super(matchVersion, input);
		}

		@Override
		protected boolean isTokenChar(int c) {
			return Character.isAlphabetic(c);
		}

		@Override
		protected int normalize(int c) {
			return Character.toLowerCase(c);
		}
	}

	@Test
	public void testTokenizer_alphabets() throws Exception {
		// look into CharTokenizer source code in advance.

		Reader reader = new StringReader("AAA12 한글 CCC");
		Tokenizer t = new CustomCharTokenizer1(Version.LUCENE_47, reader) {

			@Override
			protected boolean isTokenChar(int c) {
				return Character.isAlphabetic(c);
			}

			@Override
			protected int normalize(int c) {
				return Character.toLowerCase(c);
			}
		};

		CharTermAttribute charTermAtt;
		t.reset();

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("aaa", charTermAtt.toString());

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("한글", charTermAtt.toString());

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("ccc", charTermAtt.toString());

		assertFalse("-->", t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());

		t.close();
	}

	class CustomCharTokenizer2 extends CharTokenizer {

		public CustomCharTokenizer2(Version matchVersion, Reader input) {
			super(matchVersion, input);
		}

		@Override
		protected boolean isTokenChar(int c) {
			return !Character.isWhitespace(c);
		}

		@Override
		protected int normalize(int c) {
			return Character.toLowerCase(c);
		}
	}
	
	@Test
	public void testTokenizer_alphanumberics() throws Exception {
		// look into CharTokenizer source code in advance.

		Reader reader = new StringReader("AAA12 한글 CCC");
		CharTokenizer t = new CustomCharTokenizer2(Version.LUCENE_47, reader);

		CharTermAttribute charTermAtt;
		t.reset();

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("aaa12", charTermAtt.toString());

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("한글", charTermAtt.toString());

		assertTrue(t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("ccc", charTermAtt.toString());

		assertFalse("-->", t.incrementToken());
		charTermAtt = t.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());

		t.close();
	}

	@Test
	public void testTokenFilter() throws Exception {
		Reader reader = new StringReader("AAA12 한글 CCC");
		CharTokenizer t = new CustomCharTokenizer2(Version.LUCENE_47, reader);
		TokenFilter f = new UpperCaseFilter(Version.LUCENE_47, t);
		
		CharTermAttribute charTermAtt;
		f.reset();

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("AAA12", charTermAtt.toString());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("한글", charTermAtt.toString());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("CCC", charTermAtt.toString());

		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());

		f.close();
	}
	
	@Test
	public void testCJKBigramFilter_1() throws Exception {
		Reader reader = new StringReader("ひらがな");
		Tokenizer t = new StandardTokenizer(Version.LUCENE_47, reader);
		TokenFilter f = new CJKBigramFilter(t);
		
		CharTermAttribute charTermAtt;
		f.reset();
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("ひら", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("らが", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("がな", charTermAtt.toString());
		
		assertFalse(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());
		
		f.close();
	}
	
	@Test
	public void testCJKBigramFilter_2() throws Exception {
		Reader reader = new StringReader("지하철");
		Tokenizer t = new StandardTokenizer(Version.LUCENE_47, reader);
		TokenFilter f = new CJKBigramFilter(t);
		
		CharTermAttribute charTermAtt;
		f.reset();
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("지하", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("하철", charTermAtt.toString());
		
		assertFalse(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());
		
		f.close();
	}
	
	public void assertTotalHitCount(int expected) {
		assertEquals(expected, hits.totalHits);
	}

	public void assertField(String expected, int docIndex, String fieldName) {
		assertEquals(expected, searcher.getField(hits, docIndex, fieldName));
	}
}
