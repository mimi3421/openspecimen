package com.krishagni.catissueplus.core.audit.repository;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class RevisionsListCriteria extends AbstractListCriteria<RevisionsListCriteria> {

	private List<String> entityNames;

	private Date startDate;

	private Date endDate;

	private Long userId;

	@Override
	public RevisionsListCriteria self() {
		return this;
	}

	public List<String> entityNames() {
		return entityNames;
	}

	@JsonProperty("entityNames")
	public RevisionsListCriteria entityNames(List<String> entityNames) {
		this.entityNames = entityNames;
		return self();
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
}
