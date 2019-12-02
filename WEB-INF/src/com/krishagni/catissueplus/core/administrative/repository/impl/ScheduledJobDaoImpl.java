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
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

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
		Criteria query = getCurrentSession().createCriteria(ScheduledJobRun.class, "run")
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("run.id"));
		
		if (crit.jobId() != null) {
			query.createAlias("run.scheduledJob", "job")
				.add(Restrictions.eq("job.id", crit.jobId()));
		}

		if (crit.fromDate() != null) {
			query.add(Restrictions.ge("run.startedAt", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(Restrictions.le("run.finishedAt", crit.toDate()));
		}

		if (crit.status() != null) {
			query.add(Restrictions.eq("run.status", crit.status()));
		}
		
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds) {
		return getJobsLastRunTime(jobIds, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds, boolean onlyFinished) {
		return getJobsLastRunTime(jobIds, onlyFinished ? FINISHED_STATUSES : ALL_STATUSES);
	}

	@Override
	public Class getType() {
		return ScheduledJob.class;
	}

	private Criteria getScheduledJobsListQuery(ScheduledJobListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ScheduledJob.class);
		if (StringUtils.isNotBlank(crit.query())) {
			query.add(Restrictions.ilike("name", crit.query(), MatchMode.ANYWHERE));
		}

		if (crit.type() != null) {
			query.add(Restrictions.eq("type", crit.type()));
		}
		
		return query;
	}

	private Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds, List<String> statuses) {
		if (jobIds == null || jobIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_JOBS_LAST_RUNTIME)
			.setParameterList("jobIds", jobIds)
			.setParameterList("statuses", statuses)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Date) row[1]));
	}

	private static final String FQN = ScheduledJob.class.getName();

	private static final String GET_JOB_BY_NAME = FQN + ".getJobByName";

	private static final String GET_JOBS_LAST_RUNTIME = FQN + ".getJobsLastRuntime";

	private static final List<String> ALL_STATUSES = Arrays.asList("IN_PROGRESS", "SUCCEEDED", "FAILED");

	private static final List<String> FINISHED_STATUSES = Arrays.asList("SUCCEEDED", "FAILED");
}
