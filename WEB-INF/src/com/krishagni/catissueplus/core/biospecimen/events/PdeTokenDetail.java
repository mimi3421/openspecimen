package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

public class PdeTokenDetail {
	private String cpShortTitle;

	private String ppid;

	private Long cprId;

	private String type;

	private String formCaption;

	private Long tokenId;

	private String token;

	private String dataEntryLink;

	private Date creationTime;

	private Date expiryTime;

	private Date completionTime;

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFormCaption() {
		return formCaption;
	}

	public void setFormCaption(String formCaption) {
		this.formCaption = formCaption;
	}

	public Long getTokenId() {
		return tokenId;
	}

	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getDataEntryLink() {
		return dataEntryLink;
	}

	public void setDataEntryLink(String dataEntryLink) {
		this.dataEntryLink = dataEntryLink;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	public Date getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(Date completionTime) {
		this.completionTime = completionTime;
	}
}
