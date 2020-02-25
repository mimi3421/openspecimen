package com.krishagni.catissueplus.core.de.events;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.FormDataEntryToken;

public class FormDataEntryTokenDetail {
	private Long id;

	private Long formCtxtId;

	private Long formId;

	private String entityType;

	private Long cpId;

	private Long objectId;

	private String token;

	private UserSummary createdBy;

	private Date creationTime;

	private Date completionTime;

	private Date expiryTime;

	private String status;

	private String cpShortTitle;

	private String recordLabel;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getFormCtxtId() {
		return formCtxtId;
	}

	public void setFormCtxtId(Long formCtxtId) {
		this.formCtxtId = formCtxtId;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
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

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationTime() {
		return creationTime;
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getRecordLabel() {
		return recordLabel;
	}

	public void setRecordLabel(String recordLabel) {
		this.recordLabel = recordLabel;
	}

	public static FormDataEntryTokenDetail from(FormDataEntryToken fdeToken) {
		FormDataEntryTokenDetail result = new FormDataEntryTokenDetail();
		result.setId(fdeToken.getId());
		result.setFormCtxtId(fdeToken.getFormCtxt().getIdentifier());
		result.setFormId(fdeToken.getFormCtxt().getContainerId());
		result.setCpId(fdeToken.getFormCtxt().getCpId());
		result.setEntityType(fdeToken.getFormCtxt().getEntityType());
		result.setObjectId(fdeToken.getObjectId());
		result.setToken(fdeToken.getToken());
		result.setCreatedBy(UserSummary.from(fdeToken.getCreatedBy()));
		result.setCreationTime(fdeToken.getCreationTime());
		result.setCompletionTime(fdeToken.getCompletionTime());
		result.setExpiryTime(fdeToken.getExpiryTime());
		result.setStatus(fdeToken.getStatus().name());
		return result;
	}
}
