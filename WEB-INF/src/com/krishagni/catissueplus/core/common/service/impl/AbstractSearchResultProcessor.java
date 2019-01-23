package com.krishagni.catissueplus.core.common.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.events.SearchResult;
import com.krishagni.catissueplus.core.common.service.SearchResultProcessor;

public abstract class AbstractSearchResultProcessor implements SearchResultProcessor {
	@Override
	public List<SearchResult> search(String searchTerm, long lastId, int maxResults) {
		String query = getQuery();
		if (StringUtils.isBlank(query)) {
			return Collections.emptyList();
		}

		List<Object[]> rows = getSessionFactory().getCurrentSession().createSQLQuery(query)
			.addScalar("identifier", LongType.INSTANCE)
			.addScalar("entity", StringType.INSTANCE)
			.addScalar("entity_id", LongType.INSTANCE)
			.addScalar("name", StringType.INSTANCE)
			.addScalar("value", StringType.INSTANCE)
			.setParameter(1, searchTerm + "%")
			.setParameter(2, lastId)
			.setMaxResults(maxResults)
			.list();

		List<SearchResult> results = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = 0;
			SearchResult result = new SearchResult();
			result.setId((Long) row[idx++]);
			result.setEntity((String) row[idx++]);
			result.setEntityId((Long) row[idx++]);
			result.setKey((String) row[idx++]);
			result.setValue((String) row[idx++]);
			results.add(result);
		}

		return results;
	}

	protected String getQuery() {
		return null;
	}

	private SessionFactory getSessionFactory() {
		return OpenSpecimenAppCtxProvider.getBean("sessionFactory");
	}
}