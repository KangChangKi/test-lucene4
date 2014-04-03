package org.kang.lucene.core;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {
	private String indexDir;
	private Version version;

	private IndexWriter writer;

	private Document curDoc;

	public Indexer(String indexDir, Version version) {
		this.indexDir = indexDir;
		this.version = version;
	}

	IndexWriter makeIndexWriter() {
		try {
			File path = new File(this.indexDir);
			Directory dir = FSDirectory.open(path);
			IndexWriterConfig iwf = new IndexWriterConfig(this.version,
					new StandardAnalyzer(this.version));
			return new IndexWriter(dir, iwf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void prepareIndexWriter() {
		assert this.writer == null;

		this.writer = makeIndexWriter();

		assert this.writer != null;
	}

	public void prepareDocument() {
		assert this.curDoc == null;

		this.curDoc = new Document();

		assert this.curDoc != null;
	}

	public void addField(Field field) {
		assert this.curDoc != null;
		assert field != null;

		this.curDoc.add(field);
	}

	public void addDocument() {
		assert this.curDoc != null;

		try {
			this.writer.addDocument(this.curDoc);
			this.curDoc = null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert this.curDoc == null;
	}

	public void close() {
		assert this.writer != null;

		try {
			this.writer.close();
			this.writer = null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert this.writer == null;
	}

	public void deleteAll() {
		assert this.writer != null;

		try {
			this.writer.deleteAll();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}