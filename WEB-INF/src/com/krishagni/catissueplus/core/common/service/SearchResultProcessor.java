package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.SearchResult;

public interface SearchResultProcessor {
	String getEntity();

	List<SearchResult> search(String searchTerm, long lastId, int maxResults);
}
