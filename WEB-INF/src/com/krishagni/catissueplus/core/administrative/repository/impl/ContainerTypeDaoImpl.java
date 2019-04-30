
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTypeListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ContainerTypeDaoImpl extends AbstractDao<ContainerType> implements ContainerTypeDao {
	
	@Override
	public Class<?> getType() {
		return ContainerType.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<ContainerType> getTypes(ContainerTypeListCriteria crit) {
		return getTypesListQuery(crit)
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.asc("name"))
			.list();
	}

	@Override
	public Long getTypesCount(ContainerTypeListCriteria crit) {
		Number count = (Number) getTypesListQuery(crit)
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ContainerType> getByNames(Collection<String> names) {
		return getCurrentSession().getNamedQuery(GET_BY_NAMES)
			.setParameterList("names", names)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ContainerType getByName(String name) {
		List<ContainerType> result = getByNames(Collections.singleton(name));
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getLeafTypeIds() {
		return (List<Long>) getCurrentSession().getNamedQuery(GET_LEAF_IDS).list();
	}

	private Criteria getTypesListQuery(ContainerTypeListCriteria crit) {
		return addSearchConditions(getCurrentSession().createCriteria(ContainerType.class), crit);
	}

	private Criteria addSearchConditions(Criteria query, ContainerTypeListCriteria crit) {
		addNameRestriction(query, crit.query(), crit.matchMode());
		addCanHoldRestriction(query, crit.canHold());
		return query;
	}
	
	private void addNameRestriction(Criteria query, String name, MatchMode matchMode) {
		if (StringUtils.isBlank(name)) {
			return;
		}
		
		query.add(Restrictions.ilike("name", name, matchMode));
	}
	
	private void addCanHoldRestriction(Criteria query, List<String> canHold) {
		if (CollectionUtils.isEmpty(canHold)) {
			return;
		}
		
		query.createAlias("canHold", "canHold")
			.add(Restrictions.in("canHold.name", canHold));
	}
	
	private static final String FQN = ContainerType.class.getName();
	
	private static final String GET_BY_NAMES = FQN + ".getByNames";

	private static final String GET_LEAF_IDS = FQN + ".getLeafTypeIds";
}
