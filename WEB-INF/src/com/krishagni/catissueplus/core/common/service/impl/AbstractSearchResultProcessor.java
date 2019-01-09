package com.krishagni.catissueplus.core.common.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.events.SearchResult;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.SearchResultProcessor;

public abstract class AbstractSearchResultProcessor implements SearchResultProcessor {
	protected DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public List<SearchResult> process(List<SearchResult> matches) {
		Map<Long, SearchResult> matchesMap = new LinkedHashMap<>();
		for (SearchResult match : matches) {
			matchesMap.putIfAbsent(match.getEntityId(), match);
		}

		Map<Long, Map<String, Object>> entityProps = getEntityProps(new ArrayList<>(matchesMap.keySet()));

		Iterator<Map.Entry<Long, SearchResult>> iter = matchesMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Long, SearchResult> entry = iter.next();

			Map<String, Object> props = entityProps.get(entry.getKey());
			if (props == null) {
				iter.remove();
			} else {
				entry.getValue().setEntityProps(props);
			}
		}

		return new ArrayList<>(matchesMap.values());
	}

	protected abstract Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds);
}