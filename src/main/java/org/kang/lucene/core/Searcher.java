package org.kang.lucene.core;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Searcher {
	private String indexPath;

	private Directory directory;
	private IndexReader reader;
	private IndexSearcher searcher;

	public Searcher(String indexPath) {
		this.indexPath = indexPath;
	}

	public void prepareIndexReader() {
		assert this.directory == null;
		assert this.reader == null;

		try {
			File path = new File(this.indexPath);
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

	/**
	 * @param query		org.apache.lucene.search.Query
	 * @param filter	org.apache.lucene.search.Filter
	 * @param n			top n result
	 * @return			org.apache.lucene.search.TopDocs
	 */
	public TopDocs search(Query query, Filter filter, int n) {
		assert this.searcher != null;

		try {
			return this.searcher.search(query, filter, n);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param query		org.apache.lucene.search.Query
	 * @param n			top n result
	 * @return			org.apache.lucene.search.TopDocs
	 */
	public TopDocs search(Query query, int n) {
		assert this.searcher != null;

		try {
			return searcher.search(query, n);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void search(Query query, Filter filter, Collector results) {
		assert this.searcher != null;

		try {
			searcher.search(query, filter, results);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void search(Query query, Collector results) {
		assert this.searcher != null;

		try {
			searcher.search(query, results);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TopFieldDocs search(Query query, Filter filter, int n, Sort sort) {
		assert this.searcher != null;

		try {
			return searcher.search(query, filter, n, sort);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TopFieldDocs search(Query query, Filter filter, int n, Sort sort,
			boolean doDocScores, boolean doMaxScore) {
		assert this.searcher != null;

		try {
			return searcher.search(query, filter, n, sort, doDocScores, doMaxScore);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TopFieldDocs search(Query query, int n, Sort sort) {
		assert this.searcher != null;

		try {
			return searcher.search(query, n, sort);
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

		String result = document.get(fieldName);
		return StringUtils.defaultString(result, "");
	}
}