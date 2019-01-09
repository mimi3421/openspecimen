package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.ArrayList;
import java.util.List;

import com.krishagni.catissueplus.core.common.domain.SearchEntityKeyword;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.SearchEntityKeywordDao;

public class SearchEntityKeywordDaoImpl extends AbstractDao<SearchEntityKeyword> implements SearchEntityKeywordDao {

	@Override
	public Class<SearchEntityKeyword> getType() {
		return SearchEntityKeyword.class;
	}

	@Override
	public List<SearchEntityKeyword> getKeywords(String entity, Long entityId, String key) {
		return getCurrentSession().getNamedQuery(GET_KEYWORDS)
			.setParameter("entity", entity)
			.setParameter("entityId", entityId)
			.setParameter("key", key)
			.list();
	}

	@Override
	public List<SearchEntityKeyword> getMatches(String searchTerm, int maxResults) {
		searchTerm = searchTerm
			.replaceAll("\\\\", "\\\\\\\\")
			.replaceAll("_", "\\\\_")
			.replaceAll("%", "\\\\%");

		List<Object[]> rows = getCurrentSession().getNamedSQLQuery(GET_MATCHES)
			.setParameter("value", searchTerm + "%")
			.setMaxResults(maxResults <= 0 ? 100 : maxResults)
			.list();

		List<SearchEntityKeyword> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = 0;

			SearchEntityKeyword keyword = new SearchEntityKeyword();
			keyword.setId((Long) row[idx++]);
			keyword.setEntity((String) row[idx++]);
			keyword.setEntityId((Long) row[idx++]);
			keyword.setKey((String) row[idx++]);
			keyword.setValue((String) row[idx++]);
			keyword.setStatus((Integer) row[idx++]);
			result.add(keyword);
		}

		return result;
	}

	private static final String FQN = SearchEntityKeyword.class.getName();

	private static final String GET_KEYWORDS = FQN + ".getKeywords";

	private static final String GET_MATCHES = FQN + ".getMatches";
}
