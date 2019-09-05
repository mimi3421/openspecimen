package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.ContainerActivityLog;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ContainerActivityLogDaoImpl extends AbstractDao<ContainerActivityLog> implements ContainerActivityLogDao {
	@Override
	public Class<ContainerActivityLog> getType() {
		return ContainerActivityLog.class;
	}

	@Override
	public List<ContainerActivityLog> getActivityLogs(ContainerActivityLogListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ContainerActivityLog.class, "log")
			.createAlias("log.container", "container")
			.createAlias("log.task", "task")
			.createAlias("log.performedBy", "performedBy")
			.createAlias("log.activity", "activity", JoinType.LEFT_OUTER_JOIN)
			.add(Restrictions.eq("log.activityStatus", "Active"))
			.addOrder(Order.desc("log.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults());

		if (crit.fromDate() != null) {
			query.add(Restrictions.ge("log.activityDate", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(Restrictions.lt("log.activityDate", crit.toDate()));
		}

		if (crit.performedBy() != null) {
			query.add(Restrictions.eq("performedBy.id", crit.performedBy()));
		}

		if (crit.containerId() != null) {
			query.add(Restrictions.eq("container.id", crit.containerId()));
		}

		if (crit.taskId() != null) {
			query.add(Restrictions.eq("task.id", crit.taskId()));
		}

		if (crit.scheduledActivityId() != null) {
			query.add(Restrictions.eq("activity.id", crit.scheduledActivityId()));
		}

		return query.list();
	}

	@Override
	public Map<Long, Date> getLatestScheduledActivityDate(Collection<Long> schedActivityIds) {
		if (CollectionUtils.isEmpty(schedActivityIds)) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_LATEST_SCHED_ACTIVITY_DATE)
			.setParameterList("activityIds", schedActivityIds)
			.list();

		Map<Long, Date> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Date) row[1]);
		}

		return result;
	}

	private static final String FQN = ContainerActivityLog.class.getName();

	private static final String GET_LATEST_SCHED_ACTIVITY_DATE = FQN + ".getLatestScheduledActivityDate";
}
