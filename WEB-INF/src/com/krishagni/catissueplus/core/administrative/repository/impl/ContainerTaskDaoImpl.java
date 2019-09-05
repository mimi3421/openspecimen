package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.administrative.domain.ContainerTask;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ContainerTaskDaoImpl extends AbstractDao<ContainerTask> implements ContainerTaskDao {

	@Override
	public Class<?> getType() {
		return ContainerTask.class;
	}

	@Override
	public List<ContainerTask> getTasks(ContainerTaskListCriteria crit) {
		return getTasksListQuery(crit)
			.addOrder(Order.asc("task.name"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public Integer getTasksCount(ContainerTaskListCriteria crit) {
		return ((Number) getTasksListQuery(crit).setProjection(Projections.rowCount()).uniqueResult()).intValue();
	}

	@Override
	public ContainerTask getByName(String name) {
		return (ContainerTask) getCurrentSession().getNamedQuery(GET_BY_NAME)
			.setParameter("name", name)
			.uniqueResult();
	}

	private Criteria getTasksListQuery(ContainerTaskListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ContainerTask.class, "task");

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(Restrictions.ilike("task.name", crit.query(), MatchMode.ANYWHERE));
		}

		if (StringUtils.isNotBlank(crit.activityStatus())) {
			query.add(Restrictions.eq("task.activityStatus", crit.activityStatus()));
		}

		return query;
	}

	private static String FAQ = ContainerTask.class.getName();

	private static String GET_BY_NAME = FAQ + ".getByName";
}
