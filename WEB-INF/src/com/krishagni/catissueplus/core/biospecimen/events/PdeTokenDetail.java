package com.krishagni.catissueplus.core.biospecimen.events;

public class PdeTokenDetail {
	private String cpShortTitle;

	private String ppid;

	private Long cprId;

	private String type;

	private String formCaption;

	private String token;

	private String dataEntryLink;

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
}
