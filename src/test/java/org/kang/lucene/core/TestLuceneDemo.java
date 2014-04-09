package org.kang.lucene.core;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;
import org.junit.Ignore;

@Ignore
public class TestLuceneDemo extends LuceneTestCase {
	
	public void testDemo() throws IOException {
		Directory directory;
		{
			File file = new File("/tmp/testindex");
			directory = FSDirectory.open(file);
		}

		IndexWriter iwriter;
		{
			Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_47);
			IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_47,
					analyzer);
			iwriter = new IndexWriter(directory, conf);
		}

		String text = "This is the text to be indexed.";
		Document doc;
		{
			TextField textField = new TextField("fieldname", text, Store.YES);
			doc = new Document();
			doc.add(textField);
		}

		iwriter.deleteAll();
		iwriter.addDocument(doc);
		iwriter.close();

		IndexReader ireader = DirectoryReader.open(directory); // read-only=true
		IndexSearcher isearcher = new IndexSearcher(ireader);

		assertEquals(1, isearcher.search(new TermQuery(new Term("fieldname",
				"text to be")), 1).totalHits);

		Query query = new TermQuery(new Term("fieldname", "text"));
		TopDocs hits = isearcher.search(query, null, 1);
		assertEquals(1, hits.totalHits);

		for (int i = 0; i < hits.scoreDocs.length; i++) {
			Document hitDoc = isearcher.doc(hits.scoreDocs[i].doc);
			assertEquals(text, hitDoc.get("fieldname"));
		}

		// Test simple phrase query
		PhraseQuery phraseQuery = new PhraseQuery();
		phraseQuery.add(new Term("fieldname", "to"));
		phraseQuery.add(new Term("fieldname", "be"));
		assertEquals(2, isearcher.search(phraseQuery, null, 1).totalHits);

		ireader.close();
		directory.close();
	}

}
