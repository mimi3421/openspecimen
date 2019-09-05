package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityDao;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ScheduledContainerActivityDaoImpl extends AbstractDao<ScheduledContainerActivity> implements ScheduledContainerActivityDao {
	@Override
	public Class<ScheduledContainerActivity> getType() {
		return ScheduledContainerActivity.class;
	}

	@Override
	public List<ScheduledContainerActivity> getActivities(ScheduledContainerActivityListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ScheduledContainerActivity.class, "activity")
			.createAlias("activity.task", "task")
			.createAlias("activity.container", "container")
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.asc("activity.id"));

		if (crit.containerId() != null) {
			query.add(Restrictions.eq("container.id", crit.containerId()));
		}

		if (crit.taskId() != null) {
			query.add(Restrictions.eq("task.id", crit.taskId()));
		} else if (StringUtils.isNotBlank(crit.taskName())) {
			query.add(Restrictions.eq("task.name", crit.taskName()));
		}

		if (StringUtils.isNotBlank(crit.activityStatus())) {
			query.add(Restrictions.eq("activity.activityStatus", crit.activityStatus()));
		}

		return query.list();
	}

	@Override
	public ScheduledContainerActivity getActivity(Long containerId, String name) {
		return (ScheduledContainerActivity) getCurrentSession().createCriteria(ScheduledContainerActivity.class, "activity")
			.createAlias("activity.container", "container")
			.add(Restrictions.eq("container.id", containerId))
			.add(Restrictions.eq("activity.name", name))
			.uniqueResult();
	}
}