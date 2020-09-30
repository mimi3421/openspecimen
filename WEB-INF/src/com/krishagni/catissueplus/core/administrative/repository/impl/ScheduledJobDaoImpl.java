package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.events.JobRunsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledJobDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ScheduledJobDaoImpl extends AbstractDao<ScheduledJob> implements ScheduledJobDao {

	@Override
	@SuppressWarnings("unchecked")
	public List<ScheduledJob> getScheduledJobs(ScheduledJobListCriteria criteria) {
		return getScheduledJobsListQuery(criteria)
			.setFirstResult(criteria.startAt())
			.setMaxResults(criteria.maxResults())
			.addOrder(Order.desc("id"))
			.list();
	}

	@Override
	public Long getScheduledJobsCount(ScheduledJobListCriteria crit) {
		Number count = (Number) getScheduledJobsListQuery(crit)
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScheduledJob getJobByName(String name) {
		return (ScheduledJob) getCurrentSession().getNamedQuery(GET_JOB_BY_NAME)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	public String getRunByNodeForUpdate(Long jobId) {
		Object[] row = (Object[]) getCurrentSession().getNamedQuery(GET_RUN_BY_NODE_FOR_UPDATE)
			.setParameter("jobId", jobId)
			.uniqueResult();
		return (String) row[2];
	}

	@Override
	public int updateRunByNode(Long jobId, String node) {
		return getCurrentSession().getNamedQuery(UPDATE_RUN_BY_NODE)
			.setParameter("jobId", jobId)
			.setParameter("nodeName", node)
			.executeUpdate();
	}

	@Override
	public ScheduledJobRun getJobRun(Long id) {
		return getCurrentSession().get(ScheduledJobRun.class, id);
	}

	@Override
	public void saveOrUpdateJobRun(ScheduledJobRun job) {
		getCurrentSession().saveOrUpdate(job);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ScheduledJobRun> getJobRuns(JobRunsListCriteria crit) {
		return getCurrentSession().createCriteria(ScheduledJobRun.class, "run")
			.add(Subqueries.propertyIn("run.id", getJobRunIdsQuery(crit)))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("run.id"))
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds) {
		return getJobsLastRunTime(jobIds, ALL_STATUSES);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds, List<String> statuses) {
		if (jobIds == null || jobIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_JOBS_LAST_RUNTIME)
			.setParameterList("jobIds", jobIds)
			.setParameterList("statuses", statuses)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Date) row[1]));
	}

	@Override
	public Class getType() {
		return ScheduledJob.class;
	}

	private Criteria getScheduledJobsListQuery(ScheduledJobListCriteria crit) {
		return getCurrentSession().createCriteria(ScheduledJob.class, "job")
			.add(Subqueries.propertyIn("job.id", getJobIdsQuery(crit)));
	}

	private DetachedCriteria getJobIdsQuery(ScheduledJobListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(ScheduledJob.class, "job")
			.setProjection(Projections.distinct(Projections.property("job.id")));
		Criteria subQuery = detachedCriteria.getExecutableCriteria(getCurrentSession());

		if (StringUtils.isNotBlank(crit.query())) {
			subQuery.add(Restrictions.ilike("job.name", crit.query(), MatchMode.ANYWHERE));
		}

		if (crit.type() != null) {
			subQuery.add(Restrictions.eq("job.type", crit.type()));
		}

		if (crit.userId() != null) {
			subQuery.createAlias("job.sharedWith", "su", JoinType.LEFT_OUTER_JOIN)
				.createAlias("job.createdBy", "createdBy")
				.add(
					Restrictions.or(
						Restrictions.eq("createdBy.id", crit.userId()),
						Restrictions.eq("su.id", crit.userId())
					)
				);
		}

		return detachedCriteria;
	}

	private DetachedCriteria getJobRunIdsQuery(JobRunsListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(ScheduledJobRun.class, "run")
			.setProjection(Projections.distinct(Projections.property("run.id")));
		Criteria subQuery = detachedCriteria.getExecutableCriteria(getCurrentSession());

		if (crit.jobId() != null) {
			subQuery.createAlias("run.scheduledJob", "job")
				.add(Restrictions.eq("job.id", crit.jobId()));
		}

		if (crit.fromDate() != null) {
			subQuery.add(Restrictions.ge("run.startedAt", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			subQuery.add(Restrictions.le("run.finishedAt", crit.toDate()));
		}

		if (crit.status() != null) {
			subQuery.add(Restrictions.eq("run.status", crit.status()));
		}

		if (crit.userId() != null) {
			subQuery.createAlias("run.runBy", "runBy")
				.add(Restrictions.eq("runBy.id", crit.userId()));

			if (crit.jobId() == null) {
				subQuery.createAlias("run.scheduledJob", "job");
			}

			subQuery.createAlias("job.sharedWith", "su", JoinType.LEFT_OUTER_JOIN)
				.createAlias("job.createdBy", "createdBy")
				.add(
					Restrictions.or(
						Restrictions.eq("createdBy.id", crit.userId()),
						Restrictions.eq("su.id", crit.userId())
					)
				);
		}

		return detachedCriteria;
	}


	private static final String FQN = ScheduledJob.class.getName();

	private static final String GET_JOB_BY_NAME = FQN + ".getJobByName";

	private static final String GET_RUN_BY_NODE_FOR_UPDATE = FQN + ".getRunByNodeForUpdate";

	private static final String UPDATE_RUN_BY_NODE = FQN + ".updateRunByNode";

	private static final String GET_JOBS_LAST_RUNTIME = FQN + ".getJobsLastRuntime";

	private static final List<String> ALL_STATUSES = Arrays.asList("IN_PROGRESS", "SUCCEEDED", "FAILED");
}
