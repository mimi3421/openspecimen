package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenRequestListCriteria extends AbstractListCriteria<SpecimenRequestListCriteria> {

	private Long catalogId;

	private Collection<Long> siteIds;

	private String screeningStatus;

	private Date fromReqDate;

	private Date toReqDate;

	private Boolean pendingReqs;

	private Boolean closedReqs;

	@Override
	public SpecimenRequestListCriteria self() {
		return this;
	}

	public Long catalogId() {
		return catalogId;
	}

	public SpecimenRequestListCriteria catalogId(Long catalogId) {
		this.catalogId = catalogId;
		return this;
	}

	public Collection<Long> siteIds() {
		return siteIds;
	}

	public SpecimenRequestListCriteria siteIds(Collection<Long> siteIds) {
		this.siteIds = siteIds;
		return this;
	}

	public String screeningStatus() {
		return screeningStatus;
	}

	public SpecimenRequestListCriteria screeningStatus(String screeningStatus) {
		this.screeningStatus = screeningStatus;
		return this;
	}

	public Date fromReqDate() {
		return fromReqDate;
	}

	public SpecimenRequestListCriteria fromReqDate(Date fromReqDate) {
		this.fromReqDate = fromReqDate;
		return this;
	}

	public Date toReqDate() {
		return toReqDate;
	}

	public SpecimenRequestListCriteria toReqDate(Date toReqDate) {
		this.toReqDate = toReqDate;
		return this;
	}

	public Boolean pendingReqs() {
		return pendingReqs;
	}

	public SpecimenRequestListCriteria pendingReqs(Boolean pendingReqs) {
		this.pendingReqs = pendingReqs;
		return this;
	}

	public Boolean closedReqs() {
		return closedReqs;
	}

	public SpecimenRequestListCriteria closedReqs(Boolean closedReqs) {
		this.closedReqs = closedReqs;
		return this;
	}
}
