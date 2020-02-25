package com.krishagni.catissueplus.core.de.domain;

import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

import krishagni.catissueplus.beans.FormContextBean;

public class FormDataEntryToken extends BaseEntity {
	public enum Status {
		PENDING,

		COMPLETED,

		EXPIRED
	}

	private FormContextBean formCtxt;

	private Long objectId;

	private String token;

	private User createdBy;

	private Date creationTime;

	private Date completionTime;

	private Date expiryTime;

	private Status status;

	public FormContextBean getFormCtxt() {
		return formCtxt;
	}

	public void setFormCtxt(FormContextBean formCtxt) {
		this.formCtxt = formCtxt;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Date getCreationTime() {
		return creationTime;
	}

	@Override
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(Date completionTime) {
		this.completionTime = completionTime;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isValid() {
		return status == Status.PENDING && expiryTime.after(Calendar.getInstance().getTime());
	}
}
