package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class PdeNotif extends BaseEntity {
	private CollectionProtocolRegistration cpr;

	private Date expiryTime;

	private Set<PdeNotifLink> links = new HashSet<>();

	public CollectionProtocolRegistration getCpr() {
		return cpr;
	}

	public void setCpr(CollectionProtocolRegistration cpr) {
		this.cpr = cpr;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	public Set<PdeNotifLink> getLinks() {
		return links;
	}

	public void setLinks(Set<PdeNotifLink> links) {
		this.links = links;
	}
}
