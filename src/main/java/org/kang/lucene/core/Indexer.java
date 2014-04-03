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
	String indexPath;
	Version version;

	IndexWriter writer;

	Document curDoc;

	public Indexer(String indexPath, Version version) {
		this.indexPath = indexPath;
		this.version = version;
	}

	IndexWriter makeIndexWriter() {
		try {
			File path = new File(this.indexPath);
			Directory dir = FSDirectory.open(path);
			IndexWriterConfig iwf = new IndexWriterConfig(this.version,
					new StandardAnalyzer(this.version));
			return new IndexWriter(dir, iwf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Indexer prepareIndexWriter() {
		assert this.writer == null;

		this.writer = makeIndexWriter();

		assert this.writer != null;
		
		return this;
	}

	public Indexer prepareDocument() {
		assert this.curDoc == null;

		this.curDoc = new Document();

		assert this.curDoc != null;
		
		return this;
	}

	public Indexer addField(Field field) {
		assert this.curDoc != null;
		assert field != null;

		this.curDoc.add(field);
		
		return this;
	}

	public Indexer addDocument() {
		assert this.curDoc != null;

		try {
			this.writer.addDocument(this.curDoc);
			this.curDoc = null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assert this.curDoc == null;
		
		return this;
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

	public Indexer deleteAll() {
		assert this.writer != null;

		try {
			this.writer.deleteAll();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return this;
	}
}