package org.kang.lucene.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
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
		QueryParser parser = CoreFactory.newDefaultMultiFieldQueryParser("head", "body", "keyword");
		
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

	// not working!!!
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
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer source = new FooTokenizer(reader);
			     TokenStream filter = new FooFilter(source);
			     return new TokenStreamComponents(source, filter);
			}

		};
		
//		Analyzer a = new CustomAnalyzer();
//		QueryParser p = new QueryParser(ver, "head", a);
//		assertEquals("head:keyword", p.parse("keyword3").toString());
		
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
