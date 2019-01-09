package com.krishagni.catissueplus.core.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.common.events.SearchResult;

public interface SearchResultProcessor {
	String getEntity();

	List<SearchResult> process(List<SearchResult> matches);
}
