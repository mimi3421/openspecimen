package com.krishagni.catissueplus.core.biospecimen.domain;

public class PdeNotifLink extends BaseEntity {
	private PdeNotif notif;

	private String type;

	private Long tokenId;

	private String status;

	public PdeNotif getNotif() {
		return notif;
	}

	public void setNotif(PdeNotif notif) {
		this.notif = notif;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getTokenId() {
		return tokenId;
	}

	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
