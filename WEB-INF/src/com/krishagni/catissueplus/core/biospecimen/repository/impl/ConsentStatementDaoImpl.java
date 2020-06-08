package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.repository.ConsentStatementDao;
import com.krishagni.catissueplus.core.biospecimen.repository.ConsentStatementListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ConsentStatementDaoImpl extends AbstractDao<ConsentStatement> implements ConsentStatementDao {
	
	@Override
	public Class<?> getType() {
		return ConsentStatement.class;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ConsentStatement> getStatements(ConsentStatementListCriteria listCrit) {
		return getStatementListCriteria(listCrit)
			.addOrder(Order.asc("code"))
			.setFirstResult(listCrit.startAt())
			.setMaxResults(listCrit.maxResults())
			.list();
	}
	
	@Override
	public Long getStatementsCount(ConsentStatementListCriteria listCrit) {
		Number count = ((Number)getStatementListCriteria(listCrit)
			.setProjection(Projections.rowCount())
			.uniqueResult());
		return count.longValue();
	}
	
	@Override
	public ConsentStatement getByCode(String code) {
		return (ConsentStatement)getCurrentSession().getNamedQuery(GET_BY_CODE)
			.setString("code", code)
			.uniqueResult();
	}

	@Override
	public List<ConsentStatement> getByCodes(Collection<String> codes) {
		return getCurrentSession().createCriteria(ConsentStatement.class, "cs")
			.add(Restrictions.in("cs.code", codes))
			.list();
	}

	@Override
	public ConsentStatement getByStatement(String statement) {
		return (ConsentStatement)getCurrentSession().getNamedQuery(GET_BY_STATEMENT)
			.setString("statement", statement)
			.uniqueResult();
	}

	private Criteria getStatementListCriteria(ConsentStatementListCriteria listCrit) {
		Criteria query = getCurrentSession().createCriteria(ConsentStatement.class);
		String searchString = listCrit.query();

		if (StringUtils.isBlank(searchString)) {
			addCodeRestriction(query, listCrit.code());
			addStatementRestriction(query, listCrit.statement());
		} else {
			query.add(
				Restrictions.disjunction()
					.add(Restrictions.ilike("code",      searchString, MatchMode.ANYWHERE))
					.add(Restrictions.ilike("statement", searchString, MatchMode.ANYWHERE))
			);
		}

		return query;
	}

	private void addCodeRestriction(Criteria query, String code) {
		if (StringUtils.isNotBlank(code)) {
			query.add(Restrictions.ilike("code", code, MatchMode.ANYWHERE));
		}
	}

	private void addStatementRestriction(Criteria query, String statement) {
		if (StringUtils.isNotBlank(statement)) {
			query.add(Restrictions.ilike("statement", statement, MatchMode.ANYWHERE));
		}
	}

	private static final String FQN = ConsentStatement.class.getName();
	
	private static final String GET_BY_CODE = FQN + ".getByCode";

	private static final String GET_BY_STATEMENT = FQN + ".getByStatement";

}
