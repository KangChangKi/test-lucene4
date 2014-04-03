package org.kang.lucene.core;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class TestIndexerAndSearcher {

	@Test
	public void testIndexingAndSearching() {
		// TODO: common factory for indexer and searcher.

		// indexing
		{
			Indexer indexer = new Indexer("/tmp/testindex", Version.LUCENE_47);
			indexer.prepareIndexWriter();
			indexer.deleteAll();

			indexer.prepareDocument();
			indexer.addField(new TextField("fieldname",
					"This is the text to be indexed.", Store.YES));
			indexer.addDocument();

			indexer.prepareDocument();
			indexer.addField(new TextField("fieldname",
					"Another text to be indexed.", Store.YES));
			indexer.addDocument();

			indexer.close();
		}

		// searching
		{
			Searcher searcher = new Searcher("/tmp/testindex");
			searcher.prepareIndexReader();
			searcher.prepareIndexSearcher();

			Query query = new TermQuery(new Term("fieldname", "text"));
			TopDocs hits = searcher.search(query, null, 10);

			assertEquals(2, hits.totalHits);
			assertEquals("This is the text to be indexed.",
					searcher.getField(hits, 0, "fieldname"));
			assertEquals("Another text to be indexed.",
					searcher.getField(hits, 1, "fieldname"));

			searcher.close();
		}
	}

}
