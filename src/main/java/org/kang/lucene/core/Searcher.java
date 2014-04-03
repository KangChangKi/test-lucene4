package org.kang.lucene.core;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
	private String indexDir;

	private Directory directory;
	private IndexReader reader;
	private IndexSearcher searcher;

	public Searcher(String indexDir) {
		this.indexDir = indexDir;
	}

	public void prepareIndexReader() {
		assert this.directory == null;
		assert this.reader == null;

		try {
			File path = new File(this.indexDir);
			this.directory = FSDirectory.open(path);
			this.reader = DirectoryReader.open(this.directory);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert this.directory != null;
		assert this.reader != null;
	}

	public void prepareIndexSearcher() {
		assert this.searcher == null;

		this.searcher = new IndexSearcher(this.reader);

		assert this.searcher != null;
	}

	public void close() {
		assert this.reader != null;
		assert this.directory != null;

		try {
			this.reader.close();
			this.directory.close();

			this.reader = null;
			this.directory = null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert this.reader == null;
		assert this.directory == null;
	}

	public TopDocs search(Query query, Filter filter, int n) {
		assert this.searcher != null;

		try {
			return this.searcher.search(query, filter, n);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Document doc(int doc) {
		assert this.searcher != null;

		try {
			return this.searcher.doc(doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Document getDocument(TopDocs hits, int i) {
		assert hits != null;
		assert i >= 0;
		assert i < hits.scoreDocs.length;

		ScoreDoc sdoc = hits.scoreDocs[i];
		try {
			return searcher.doc(sdoc.doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getField(TopDocs hits, int i, String fieldName) {
		assert fieldName != null;
		assert fieldName.length() > 0;

		Document document = getDocument(hits, i);
		assert document != null;

		return document.get(fieldName);
	}
}