package com.krishagni.catissueplus.core.common.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
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

	@Override
	public Map<Long, Map<String, Object>> getEntityProps(Collection<Long> entityIds) {
		String propsQuery = getEntityPropsQuery();
		if (StringUtils.isBlank(propsQuery) || CollectionUtils.isEmpty(entityIds)) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = getSessionFactory().getCurrentSession().createSQLQuery(propsQuery)
			.addScalar("entityId", LongType.INSTANCE)
			.addScalar("name", StringType.INSTANCE)
			.addScalar("value", StringType.INSTANCE)
			.setParameterList("entityIds", entityIds)
			.list();

		Map<Long, Map<String, Object>> result = new HashMap<>();
		for (Object[] row : rows) {
			int idx = -1;
			Long entityId = (Long) row[++idx];
			Map<String, Object> entityProps = result.computeIfAbsent(entityId, (k) -> new HashMap<>());
			entityProps.put((String) row[++idx], row[++idx]);
		}

		return result;
	}

	protected String getQuery() {
		return null;
	}

	protected String getEntityPropsQuery() {
		return null;
	}

	private SessionFactory getSessionFactory() {
		return OpenSpecimenAppCtxProvider.getBean("sessionFactory");
	}
}