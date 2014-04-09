package org.kang.lucene.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.core.UpperCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tartarus.snowball.SnowballProgram;

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

	@Test
	public void testStandardAnalyzer() throws Exception {
		Analyzer a = new StandardAnalyzer(Version.LUCENE_47);
		TokenStream ts = a
				.tokenStream("contents", new StringReader("aaa 공항철도"));
		CharTermAttribute cta = ts.addAttribute(CharTermAttribute.class);
		PositionIncrementAttribute pia = ts
				.addAttribute(PositionIncrementAttribute.class);

		ts.reset();

		List<String> list = new ArrayList<String>();
		List<Object> list2 = new ArrayList<Object>();
		while (ts.incrementToken()) {
			list.add(cta.toString());
			list2.add(pia.getPositionIncrement());
		}

		String result = StringUtils.join(list, ",");
		String result2 = StringUtils.join(list2, ",");
		assertEquals("aaa,공항철도", result);
		assertEquals("1,1", result2);

		a.close();
	}

	public static interface SynonymEngine {
		String[] getSynonyms(String s) throws IOException;
	}

	public static final class SynonymFilter extends TokenFilter {
		public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";

		private Stack<String> synonymStack;
		private SynonymEngine engine;
		private AttributeSource.State current;

		private final CharTermAttribute termAtt;
		private final PositionIncrementAttribute posIncrAtt;

		public SynonymFilter(TokenStream in, SynonymEngine engine) {
			super(in);
			synonymStack = new Stack<String>();
			this.engine = engine;

			this.termAtt = addAttribute(CharTermAttribute.class);
			this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
		}

		public boolean incrementToken() throws IOException {
			if (synonymStack.size() > 0) {
				String syn = synonymStack.pop();
				restoreState(current);

				// termAtt.setTermBuffer(syn);
				termAtt.setEmpty();
				termAtt.append(syn);

				posIncrAtt.setPositionIncrement(0);
				return true;
			}

			if (!input.incrementToken())
				return false;

			if (addAliasesToStack()) {
				current = captureState();
			}

			return true;
		}

		private boolean addAliasesToStack() throws IOException {
			String[] synonyms = engine.getSynonyms(termAtt.toString());
			if (synonyms == null) {
				return false;
			}
			for (String synonym : synonyms) {
				synonymStack.push(synonym);
			}
			return true;
		}
	}

	public static class TestSynonymEngine implements SynonymEngine {
		private static HashMap<String, String[]> map = new HashMap<String, String[]>();

		static {
			map.put("quick", new String[] { "fast", "speedy" });
			map.put("jumps", new String[] { "leaps", "hops" });
			map.put("over", new String[] { "above" });
			map.put("lazy", new String[] { "apathetic", "sluggish" });
			map.put("dog", new String[] { "canine", "pooch" });
		}

		public String[] getSynonyms(String s) {
			return map.get(s);
		}
	}

	@Test
	public void testSynonymFilter_1() throws Exception {
		Reader reader = new StringReader("over apple");
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		TokenFilter f = new SynonymFilter(t, new TestSynonymEngine());

		CharTermAttribute charTermAtt;
		PositionIncrementAttribute posIncAttr;
		f.reset();

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("over", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("above", charTermAtt.toString());
		assertEquals("-->", 0, posIncAttr.getPositionIncrement());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("apple", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		f.close();
	}

	/**
	 * synonym engine implementation with another lucene index.
	 *
	 */
	public static class WordNetSynonymEngine implements SynonymEngine {
		IndexSearcher searcher;
		Directory fsDir;

		public WordNetSynonymEngine(File index) throws IOException {
			fsDir = FSDirectory.open(index);
			searcher = new IndexSearcher(DirectoryReader.open(fsDir));
		}

		public void close() throws IOException {
			// searcher.close();
			fsDir.close();
		}

		public String[] getSynonyms(String word) throws IOException {
			List<String> synList = new ArrayList<String>();

			TopDocs topDocs = searcher.search(new TermQuery(new Term("word",
					word)), 10);

			for (ScoreDoc hit : topDocs.scoreDocs) {
				Document doc = searcher.doc(hit.doc);

				String[] values = doc.getValues("syn");

				for (String syn : values) {
					synList.add(syn);
				}
			}

			return synList.toArray(new String[0]);
		}
	}

	// 1. remove stop words
	// 2. remove prefixes and suffixes
	// 3. apply EdgeNGramFilter(including original word) or morphological analysis
	// 4. apply synonyms
	// 5. apply sound-like filter(FuzzyFilter)
	// 6. map consonants
	
	@Ignore
	@Test
	public void testSynonymFilter_2() throws Exception {
		Reader reader = new StringReader("over apple");
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		File file = null; // TODO: create the index for synonyms.
		TokenFilter f = new SynonymFilter(t, new WordNetSynonymEngine(file));

		CharTermAttribute charTermAtt;
		PositionIncrementAttribute posIncAttr;
		f.reset();

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("over", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("above", charTermAtt.toString());
		assertEquals("-->", 0, posIncAttr.getPositionIncrement());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("apple", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		posIncAttr = f.getAttribute(PositionIncrementAttribute.class);
		assertEquals("", charTermAtt.toString());
		assertEquals(1, posIncAttr.getPositionIncrement());

		f.close();
	}
	
	@Test
	public void testSnowball_1() throws Exception {
		Reader reader = new StringReader("stemming algorithms");
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		SnowballFilter f = new SnowballFilter(t, "English");
		
		CharTermAttribute charTermAtt;
		f.reset();

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("stem", charTermAtt.toString());

		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("algorithm", charTermAtt.toString());

		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());
		
		f.close();
	}
	
	@Test
	public void testSnowball_2() throws Exception {
		Reader reader = new StringReader("뛰어 갈까");
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		
		/* from EnglishSnowballFilter class:
		  @Override
		  public final boolean incrementToken() throws IOException {
		    if (input.incrementToken()) {
		      if (!keywordAttr.isKeyword()) {
		        char termBuffer[] = termAtt.buffer();
		        final int length = termAtt.length();
		        stemmer.setCurrent(termBuffer, length);
		        stemmer.stem();
		        final char finalTerm[] = stemmer.getCurrentBuffer();
		        final int newLength = stemmer.getCurrentBufferLength();
		        if (finalTerm != termBuffer)
		          termAtt.copyBuffer(finalTerm, 0, newLength);
		        else
		          termAtt.setLength(newLength);
		      }
		      return true;
		    } else {
		      return false;
		    }
		  }
		 */
		
		SnowballProgram stemmer = new SnowballProgram() {

			@Override
			public boolean stem() {
				if (StringUtils.equals(getCurrent(), "뛰어")) {
					setCurrent("뛰다");
					return true;
				} else if (StringUtils.equals(getCurrent(), "갈까")) {
					setCurrent("가다");
					return true;
				}

				return false;
			}
			
		};
		SnowballFilter f = new SnowballFilter(t, stemmer);
		
		CharTermAttribute charTermAtt;
		f.reset();
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("뛰다", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("가다", charTermAtt.toString());
		
		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());
		
		f.close();
	}

	@Test
	public void testNGramTokenFilter() throws Exception {
		String s = "국제공항";
		Reader reader = new StringReader(s);
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		int minGram = 2;
		int maxGram = s.length();
		TokenFilter f = new NGramTokenFilter(Version.LUCENE_47, t, minGram, maxGram);
		
		CharTermAttribute charTermAtt;
		f.reset();
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제", charTermAtt.toString()); // <-- correct.
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제공", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제공항", charTermAtt.toString()); // <-- correct.
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("제공", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("제공항", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("공항", charTermAtt.toString()); // <-- correct.
		
		assertFalse("-->", f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("", charTermAtt.toString());
		
		f.close();
	}
	
	@Test
	public void testEdgeNGramTokenFilter() throws Exception {
		String s = "국제공항";
		Reader reader = new StringReader(s);
		Tokenizer t = new WhitespaceTokenizer(Version.LUCENE_47, reader);
		int minGram = 2;
		int maxGram = s.length();
		TokenFilter f = new EdgeNGramTokenFilter(Version.LUCENE_47, t, minGram, maxGram); // front! back is deprecated.
		
		CharTermAttribute charTermAtt;
		f.reset();
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제", charTermAtt.toString()); // <-- correct.
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제공", charTermAtt.toString());
		
		assertTrue(f.incrementToken());
		charTermAtt = f.getAttribute(CharTermAttribute.class);
		assertEquals("국제공항", charTermAtt.toString()); // <-- correct.
		
		// but the missing of "공항" has made.
		
		assertFalse("-->", f.incrementToken());
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
