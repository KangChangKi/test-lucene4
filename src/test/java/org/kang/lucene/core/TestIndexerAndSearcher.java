package org.kang.lucene.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
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
import org.junit.Ignore;
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
	private void getToken(Reader input, CharBuffer cbuff, StringBuffer strbuff) throws Exception {
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
	
	@Test
	public void testTokenizer_1() throws Exception {
		StringReader input = new StringReader("aaa.#@?한글   \n   \t  ccc");
		
		final int bufferSize = 2;
		CharBuffer cbuff = CharBuffer.allocate(bufferSize);
		assertEquals(bufferSize, cbuff.capacity());
		assertEquals(bufferSize, cbuff.limit());
		assertEquals(0, cbuff.position());

		StringBuffer strbuff = new StringBuffer();
		
		getToken(input, cbuff, strbuff);
		assertEquals("aaa", strbuff.toString());

		getToken(input, cbuff, strbuff);
		assertEquals("한글", strbuff.toString());

		getToken(input, cbuff, strbuff);
		assertEquals("ccc", strbuff.toString());

		getToken(input, cbuff, strbuff);
		assertEquals("", strbuff.toString());
	}

	@Test
	public void testTokenizer_2() throws Exception {		
		final class SomeTokenizer extends Tokenizer {

			// register attributes to the thread local.
			private CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class);
			private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
			private TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
			private PositionIncrementAttribute positionIncAtt = addAttribute(PositionIncrementAttribute.class);

			public static final int BUFFER_CAPACITY = 3000; // for whole content.
			CharBuffer cbuff = CharBuffer.allocate(BUFFER_CAPACITY);
			int contentLength = 0;
			StringBuffer strbuff = new StringBuffer(); // for each token.
			
			protected SomeTokenizer(Reader input) {
				super(input); // this.input
			}
			
			@Override
			public void reset() throws IOException {
				super.reset();
				
				contentLength = 0;
			}
			
			@Override
			public boolean incrementToken() throws IOException {
				clearAttributes();

				getToken(input, cbuff, strbuff); 
				charTermAtt.append(strbuff.toString());
				
				return strbuff.length() != 0;
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
			private void getToken(Reader input, CharBuffer cbuff, StringBuffer strbuff) throws IOException {
				strbuff.delete(0, strbuff.length());
				
				int res = input.read(cbuff);
				if (res == -1) {
					// read all already.
				} else {
					contentLength = res;
				}
				cbuff.flip();

				while (true) {
					char c = cbuff.get();
					--contentLength;
					
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
		}
		
		Reader input = new StringReader("aaa bbb");
		SomeTokenizer t = new SomeTokenizer(input);
		
		CharTermAttribute charTermAtt = t.getAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = t.getAttribute(OffsetAttribute.class);
		TypeAttribute typeAtt = t.getAttribute(TypeAttribute.class);
		PositionIncrementAttribute positionIncAtt = t.getAttribute(PositionIncrementAttribute.class);
		
		t.reset(); // stage input reader. 
		
		assertTrue(t.incrementToken());
		assertEquals("aaa", charTermAtt.toString());
		
		assertTrue(t.incrementToken());
		assertEquals("bbb", charTermAtt.toString());
		
		assertTrue(t.incrementToken());
		assertEquals("", charTermAtt.toString());
	}
	
	@Ignore
	@Test
	public void testTokenizer_3() throws Exception {
		final class SomeTokenizer extends Tokenizer {

			private CharBuffer cbuff = CharBuffer.allocate(512);

			// register attributes to the thread local.
			private CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class);
			private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
			private TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
			private PositionIncrementAttribute positionIncAtt = addAttribute(PositionIncrementAttribute.class);

			protected SomeTokenizer(Reader input) {
				super(input); // this.input
			}

			@Override
			public boolean incrementToken() throws IOException {
				clearAttributes();

				input.read(cbuff);

				cbuff.flip();

				char c = cbuff.get();
				if (Character.isAlphabetic(c)) {

				} else {

				}

				endOfReading();

				return false;
			}

			private boolean hasNext() {
				return cbuff.position() < cbuff.limit();
			}

			private void endOfReading() {
				if (cbuff.position() == cbuff.limit()) {
					cbuff.clear();
				} else {
					cbuff.compact();
				}
			}
		}

	}

	@Test
	public void testSearchingWithCustomAnalyzer() throws Exception {
		final Version ver = Version.LUCENE_47;

		final class FooTokenizer extends Tokenizer {

			protected FooTokenizer(Reader input) {
				super(input);
			}

			@Override
			public boolean incrementToken() throws IOException {
				if (input.read() != -1) {
					return true;
				}
				return false;
			}

		}

		final class FooFilter extends TokenStream {

			private Tokenizer source;

			public FooFilter(Tokenizer source) {
				this.source = source;
			}

			@Override
			public boolean incrementToken() throws IOException {
				return source.incrementToken();
			}

		}

		class CustomAnalyzer extends Analyzer {

			private TokenStream tokenStream;

			public CustomAnalyzer() {
				super();
			}

			@Override
			protected TokenStreamComponents createComponents(String fieldName,
					Reader reader) {
				Tokenizer source = new FooTokenizer(reader);
				TokenStream filter = new FooFilter(source);
				return new TokenStreamComponents(source, filter);
			}

		}
		;

		// Analyzer a = new CustomAnalyzer();
		// QueryParser p = new QueryParser(ver, "head", a);
		// assertEquals("head:keyword", p.parse("keyword3").toString());

		QueryParser parser = new QueryParser(ver, "head", new CustomAnalyzer());

		query = parser.parse("title");
		assertEquals("head:title", query.toString());
		hits = searcher.search(query, 10);
		assertTotalHitCount(2);
		assertField("title title1", 0, "head");
		assertField("title title2", 1, "head");

		query = parser.parse("title AND title1");
		assertEquals("+head:title +head:title1", query.toString());
		hits = searcher.search(query, 10);
		assertTotalHitCount(1);
		assertField("title title1", 0, "head");
	}

	public void assertTotalHitCount(int expected) {
		assertEquals(expected, hits.totalHits);
	}

	public void assertField(String expected, int docIndex, String fieldName) {
		assertEquals(expected, searcher.getField(hits, docIndex, fieldName));
	}
}
