package org.kang.lucene.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
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

	/**
	 * config
	 */
	// @formatter:off
	static {
		addConfig(
				"default"
				, "path", "index/testindex"
				, "version", Version.LUCENE_47
				, "analyzer", new String[] {
						"org.apache.lucene.analysis.standard.StandardAnalyzer"
						, "version" }
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

	private static <T> T makeInstance(String configName, String[] spec, Class<T> type) {
		assert spec.length > 0;
		
		String className = spec[0];
		
		List<Class<?>> argTypes = new ArrayList<Class<?>>();
		List<Object> argObjects = new ArrayList<Object>();
		for (int i=1; i<spec.length; i++) {
			String name = spec[i];
			Object each = getConfig(configName, name, Object.class);
			
			argTypes.add(each.getClass());
			argObjects.add(each);
		}
		
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(argTypes.toArray(new Class[0]));
			Object newInstance = constructor.newInstance(argObjects.toArray());
			
			return type.cast(newInstance);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static QueryParser newMultiFieldQueryParser(String configName, String... fieldNames) {
		assert fieldNames.length > 0;
		
		Version version = getConfig(configName, "version", Version.class);
		String[] analyzerSpec = getConfig(configName, "analyzer", String[].class);

		Analyzer analyzer = makeInstance(configName, analyzerSpec, Analyzer.class);
		MultiFieldQueryParser parser = new MultiFieldQueryParser(version, fieldNames, analyzer);
		
		return parser;
		
	}
	
	public static Indexer newDefaultIndexer() {
		return newIndexer("default");
	}

	public static Searcher newDefaultSearcher() {
		return newSearcher("default");
	}
	
	public static QueryParser newDefaultMultiFieldQueryParser(String... fieldNames) {
		return newMultiFieldQueryParser("default", fieldNames);
	}
}
