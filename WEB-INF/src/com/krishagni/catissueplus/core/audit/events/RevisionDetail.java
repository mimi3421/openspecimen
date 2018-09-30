package com.krishagni.catissueplus.core.audit.events;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class RevisionDetail {
	private Long revisionId;

	private UserSummary changedBy;

	private Date changedOn;

	private List<RevisionEntityRecordDetail> records;

	public Long getRevisionId() {
		return revisionId;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}

	public UserSummary getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(UserSummary changedBy) {
		this.changedBy = changedBy;
	}

	public Date getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Date changedOn) {
		this.changedOn = changedOn;
	}

	public List<RevisionEntityRecordDetail> getRecords() {
		return records;
	}

	public void setRecords(List<RevisionEntityRecordDetail> records) {
		this.records = records;
	}

	public Long getLastRecordId() {
		return (records != null && !records.isEmpty()) ? records.get(records.size() - 1).getId() : 0L;
	}
}
