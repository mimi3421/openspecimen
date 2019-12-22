package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.MobileUploadJob;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.MobileUploadJobDao;
import com.krishagni.catissueplus.core.common.repository.MobileUploadJobsListCriteria;

public class MobileUploadJobDaoImpl extends AbstractDao<MobileUploadJob> implements MobileUploadJobDao {
	public Class<MobileUploadJob> getType() {
		return MobileUploadJob.class;
	}

	@Override
	public List<MobileUploadJob> getJobs(MobileUploadJobsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(MobileUploadJob.class, "job")
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("job.id"));

		if (crit.instituteId() != null) {
			query.createAlias("job.createdBy", "user")
				.createAlias("user.institute", "institute")
				.add(Restrictions.eq("institute.id", crit.instituteId()));
		}

		if (crit.cpId() != null) {
			query.createAlias("job.cp", "cp")
				.add(Restrictions.eq("cp.id", crit.cpId()));
		}

		if (crit.userId() != null) {
			if (crit.instituteId() == null) {
				// avoid re-creating user alias
				query.createAlias("job.createdBy", "user")
			}

			query.add(Restrictions.eq("user.id", crit.userId()));
		}

		return query.list();
	}
}
