package com.krishagni.catissueplus.core.audit.repository;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class RevisionsListCriteria extends AbstractListCriteria<RevisionsListCriteria> {

	private Date startDate;

	private Date endDate;

	private Long userId;

	private boolean includeModifiedProps;

	@Override
	public RevisionsListCriteria self() {
		return this;
	}

	public Date startDate() {
		return startDate;
	}

	@JsonProperty("startDate")
	public RevisionsListCriteria startDate(Date startDate) {
		this.startDate = startDate;
		return self();
	}

	public Date endDate() {
		return endDate;
	}

	@JsonProperty("endDate")
	public RevisionsListCriteria endDate(Date endDate) {
		this.endDate = endDate;
		return self();
	}

	public Long userId() {
		return userId;
	}

	@JsonProperty("userId")
	public RevisionsListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public boolean includeModifiedProps() {
		return includeModifiedProps;
	}

	@JsonProperty("includeModifiedProps")
	public RevisionsListCriteria includeModifiedProps(boolean includeModifiedProps) {
		this.includeModifiedProps = includeModifiedProps;
		return self();
	}

	public String toString() {
		return new StringBuilder().append(super.toString()).append(", ")
			.append("start date = ").append(startDate()).append(", ")
			.append("end date = ").append(endDate()).append(", ")
			.append("user = ").append(userId()).append(", ")
			.append("include modified props = ").append(includeModifiedProps())
			.toString();
	}
}
