package com.krishagni.catissueplus.core.common.domain;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class MobileUploadJob extends BaseEntity {
	public enum Status {
		COMPLETED,
		FAILED,
		QUEUED,
		IN_PROGRESS,
		STOPPED
	}

	private CollectionProtocol cp;

	private Status status;

	private Long totalRecords;

	private Long failedRecords;

	private User createdBy;

	private Date creationTime;

	private Date endTime;

	private transient Map<String, Object> cache = new HashMap<>();

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
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

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Date getCreationTime() {
		return creationTime;
	}

	@Override
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public File getWorkingDir() {
		File mobileAppDir = new File(ConfigUtil.getInstance().getDataDir(), "mobile-app");
		File jobsDir = new File(mobileAppDir, "jobs");
		File jobDir = new File(jobsDir, getId().toString());
		if (!jobDir.exists()) {
			jobDir.mkdirs();
		}

		return jobDir;
	}

	public File getInputDir() {
		return new File(getWorkingDir(), "input");
	}

	public File getOutputDir() {
		return new File(getWorkingDir(), "output");
	}

	public void putInCache(String key, Object object) {
		cache.put(key, object);
	}

	public Object getFromCache(String key) {
		return cache.get(key);
	}
}
