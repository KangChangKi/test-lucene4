package org.kang.lucene.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.util.Version;

public final class CoreFactory {

	private CoreFactory() {
	}

	public static final Map<String, Map<String, Object>> config = new HashMap<String, Map<String, Object>>();

	public static void addConfig(String configName, Object... pairs) {
		assert configName != null;
		assert configName.length() > 0;
		assert pairs.length > 0;
		assert pairs.length % 2 == 0;

		Map<String, Object> values = new HashMap<String, Object>();

		for (int i = 0; i < pairs.length; i += 2) {
			String key = (String) pairs[i];
			Object value = pairs[i + 1];

			values.put(key, value);
		}

		config.put("default", values);
	}

	public static <T> T getConfig(String configName, String key, Class<T> type) {
		Map<String, Object> map = config.get(configName);
		assert map != null;

		Object object = map.get(key);
		assert object != null;

		return type.cast(object);
	}

	// @formatter:off
	static {
		addConfig(
				"default"
				, "path", "/tmp/testindex"
				, "version", Version.LUCENE_47
				
				// add other config here.
				);
	}
	// @formatter:on

	public static Indexer newIndexer(String configName) {
		String path = getConfig(configName, "path", String.class);
		Version version = getConfig(configName, "version", Version.class);
		
		Indexer indexer = new Indexer(path, version);
		indexer.prepareIndexWriter();

		return indexer;
	}

	public static Searcher newSearcher(String configName) {
		String path = getConfig(configName, "path", String.class);
		
		Searcher searcher = new Searcher(path);
		searcher.prepareIndexReader();
		searcher.prepareIndexSearcher();

		return searcher;
	}

	public static Indexer newIndexer() {
		return newIndexer("default");
	}

	public static Searcher newSearcher() {
		return newSearcher("default");
	}
}
