package com.krishagni.catissueplus.core.importer.repository.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;

public class ImportJobDaoImpl extends AbstractDao<ImportJob> implements ImportJobDao {
	
	public Class<ImportJob> getType() {
		return ImportJob.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ImportJob> getImportJobs(ListImportJobsCriteria crit) {
		int startAt = crit.startAt() <= 0 ? 0 : crit.startAt();
		int maxResults = crit.maxResults() <= 0 || crit.maxResults() > 100 ? 100 : crit.maxResults();
		
		Criteria query = getCurrentSession().createCriteria(ImportJob.class)
			.setFirstResult(startAt)
			.setMaxResults(maxResults)
			.addOrder(Order.desc("id"));

		if (StringUtils.isNotBlank(crit.status())) {
			try {
				query.add(Restrictions.eq("status", ImportJob.Status.valueOf(crit.status())));
			} catch (Exception e) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST, e.getMessage());
			}
		}

		if (crit.instituteId() != null) {
			query.createAlias("createdBy", "createdBy")
				.createAlias("createdBy.institute","institute")
				.add(Restrictions.eq("institute.id", crit.instituteId()));
		} else if (crit.userId() != null) {
			query.createAlias("createdBy", "createdBy")
				.add(Restrictions.eq("createdBy.id", crit.userId()));
		}
		
		if (CollectionUtils.isNotEmpty(crit.objectTypes())) {
			query.add(Restrictions.in("name", crit.objectTypes()));
		}

		if (crit.params() != null && !crit.params().isEmpty()) {
			query.createAlias("params", "params");

			Disjunction orCond = Restrictions.disjunction();
			query.add(orCond);

			for (Map.Entry<String, String> kv : crit.params().entrySet()) {
				orCond.add(Restrictions.and(
					Restrictions.eq("params.indices", kv.getKey()),
					Restrictions.eq("params.elements", kv.getValue())
				));
			}
		}
						
		return query.list();
	}

	@Override
	public int markInProgressJobsAsFailed() {
		return getCurrentSession().createSQLQuery(MARK_IN_PROGRESS_JOBS_AS_FAILED_SQL).executeUpdate();
	}

	private static final String MARK_IN_PROGRESS_JOBS_AS_FAILED_SQL = "update os_bulk_import_jobs set status = 'STOPPED' where status = 'IN_PROGRESS'";
}
