package com.krishagni.catissueplus.core.common.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class MobileUploadJobsListCriteria extends AbstractListCriteria<MobileUploadJobsListCriteria> {

	private Long userId;

	private Long cpId;

	private Long instituteId;

	@Override
	public MobileUploadJobsListCriteria self() {
		return this;
	}

	public Long userId() {
		return userId;
	}

	public MobileUploadJobsListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Long cpId() {
		return cpId;
	}

	public MobileUploadJobsListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public MobileUploadJobsListCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}
}
