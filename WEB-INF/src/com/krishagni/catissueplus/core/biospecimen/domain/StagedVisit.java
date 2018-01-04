package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

public class StagedVisit extends Visit {
	private Date updateTime;

	private String eventLabel;

	private String collectionSite;

	private StagedParticipant participant;

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public String getCollectionSite() {
		return collectionSite;
	}

	public void setCollectionSite(String collectionSite) {
		this.collectionSite = collectionSite;
	}

	public StagedParticipant getParticipant() {
		return participant;
	}

	public void setParticipant(StagedParticipant participant) {
		this.participant = participant;
	}
}
