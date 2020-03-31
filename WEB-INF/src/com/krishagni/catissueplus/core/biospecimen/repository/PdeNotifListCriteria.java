package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class PdeNotifListCriteria extends AbstractListCriteria<PdeNotifListCriteria> {

	private Long cpId;

	private String status;

	private Date minCreationTime;

	private Date maxCreationTime;

	private String ppid;

	@Override
	public PdeNotifListCriteria self() {
		return this;
	}

	public Long cpId() {
		return cpId;
	}

	public PdeNotifListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public String status() {
		return status;
	}

	public PdeNotifListCriteria status(String status) {
		this.status = status;
		return self();
	}

	public Date minCreationTime() {
		return minCreationTime;
	}

	public PdeNotifListCriteria minCreationTime(Date minCreationTime) {
		this.minCreationTime = minCreationTime;
		return self();
	}

	public Date maxCreationTime() {
		return maxCreationTime;
	}

	public PdeNotifListCriteria maxCreationTime(Date maxCreationTime) {
		this.maxCreationTime = maxCreationTime;
		return self();
	}

	public String ppid() {
		return ppid;
	}

	public PdeNotifListCriteria ppid(String ppid) {
		this.ppid = ppid;
		return self();
	}
}
