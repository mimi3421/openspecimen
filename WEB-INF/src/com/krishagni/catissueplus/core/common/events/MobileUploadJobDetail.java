package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.common.domain.MobileUploadJob;

public class MobileUploadJobDetail {
	private Long id;

	private String cpShortTitle;

	private Long cpId;

	private UserSummary createdBy;

	private Date creationTime;

	private Date endTime;

	private Long totalRecords;

	private Long failedRecords;

	private String status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public Long getFailedRecords() {
		return failedRecords;
	}

	public void setFailedRecords(Long failedRecords) {
		this.failedRecords = failedRecords;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public static List<MobileUploadJobDetail> from(Collection<MobileUploadJob> jobs) {
		return jobs.stream().map(MobileUploadJobDetail::from).collect(Collectors.toList());
	}

	public static MobileUploadJobDetail from(MobileUploadJob job) {
		MobileUploadJobDetail result = new MobileUploadJobDetail();
		result.setId(job.getId());
		result.setCpId(job.getCp().getId());
		result.setCpShortTitle(job.getCp().getShortTitle());
		result.setCreatedBy(UserSummary.from(job.getCreatedBy()));
		result.setCreationTime(job.getCreationTime());
		result.setEndTime(job.getEndTime());
		result.setStatus(job.getStatus().name());
		result.setTotalRecords(job.getTotalRecords());
		result.setFailedRecords(job.getFailedRecords());
		return result;
	}
}
