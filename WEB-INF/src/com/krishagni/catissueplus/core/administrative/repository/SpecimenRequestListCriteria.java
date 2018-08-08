package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenRequestListCriteria extends AbstractListCriteria<SpecimenRequestListCriteria> {

	private Long catalogId;

	private Date fromReqDate;

	private Date toReqDate;

	private String status;

	//
	// internally added restrictions
	//
	private Collection<SiteCpPair> sites;

	private Long requestorId;


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

	public String status() {
		return status;
	}

	public SpecimenRequestListCriteria status(String status) {
		this.status = status;
		return self();
	}

	public Collection<SiteCpPair> sites() {
		return sites;
	}

	public SpecimenRequestListCriteria sites(Collection<SiteCpPair> sites) {
		this.sites = sites;
		return this;
	}

	public Long requestorId() {
		return requestorId;
	}

	public SpecimenRequestListCriteria requestorId(Long requestorId) {
		this.requestorId = requestorId;
		return self();
	}
}
