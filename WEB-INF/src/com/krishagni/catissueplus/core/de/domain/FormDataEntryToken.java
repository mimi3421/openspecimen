package com.krishagni.catissueplus.core.de.domain;

import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.DataEntryToken;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

import krishagni.catissueplus.beans.FormContextBean;

public class FormDataEntryToken extends BaseEntity implements DataEntryToken {
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

	@Override
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
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

	@Override
	public Date getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(Date completionTime) {
		this.completionTime = completionTime;
	}

	@Override
	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isValid() {
		return status == Status.PENDING && expiryTime.after(Calendar.getInstance().getTime());
	}

	@Override
	public String getUrl() {
		return ConfigUtil.getInstance().getAppUrl() + "/#/patient-data-entry?token=" + getToken();
	}
}
