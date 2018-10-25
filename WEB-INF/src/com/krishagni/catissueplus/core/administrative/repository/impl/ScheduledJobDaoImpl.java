package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
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
	public List<ScheduledJobRun> getJobRuns(JobRunsListCriteria listCriteria) {
		Criteria criteria = getCurrentSession().createCriteria(ScheduledJobRun.class)
			.setFirstResult(listCriteria.startAt())
			.setMaxResults(listCriteria.maxResults())
			.addOrder(Order.desc("id"));
		
		if (listCriteria.scheduledJobId() != null) {
			criteria.createAlias("scheduledJob", "job");
			criteria.add(Restrictions.eq("job.id", listCriteria.scheduledJobId()));
		}
		
		return criteria.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_JOBS_LAST_RUNTIME)
			.setParameterList("jobIds", jobIds)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Date) row[1]));
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
		
		return query;
	}

	private static final String FQN = ScheduledJob.class.getName();

	private static final String GET_JOB_BY_NAME = FQN + ".getJobByName";

	private static final String GET_JOBS_LAST_RUNTIME = FQN + ".getJobsLastRuntime";
}
