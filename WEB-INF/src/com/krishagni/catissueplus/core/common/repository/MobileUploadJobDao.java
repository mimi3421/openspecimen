package com.krishagni.catissueplus.core.common.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.MobileUploadJob;

public interface MobileUploadJobDao extends Dao<MobileUploadJob> {
	List<MobileUploadJob> getJobs(MobileUploadJobsListCriteria crit);
}
