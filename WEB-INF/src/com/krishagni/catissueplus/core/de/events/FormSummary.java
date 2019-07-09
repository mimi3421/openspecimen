package com.krishagni.catissueplus.core.de.events;

import java.util.Date;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.Form;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class FormSummary {
	private Long formId;
	
	private String name;
	
	private String caption;
	
	private UserSummary createdBy;
	
	private Date creationTime;
	
	private Date modificationTime;
	
	private Integer cpCount;
	
	private boolean sysForm;

	private String entityType;

	private boolean multipleRecords;

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
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

	public Date getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(Date modificationTime) {
		this.modificationTime = modificationTime;
	}

	public Integer getCpCount() {
		return cpCount;
	}

	public void setCpCount(Integer cpCount) {
		this.cpCount = cpCount;
	}

	public boolean isSysForm() {
		return sysForm;
	}

	public void setSysForm(boolean sysForm) {
		this.sysForm = sysForm;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public boolean isMultipleRecords() {
		return multipleRecords;
	}

	public void setMultipleRecords(boolean multipleRecords) {
		this.multipleRecords = multipleRecords;
	}

	public static FormSummary from(Form form) {
		FormSummary result = new FormSummary();
		result.setFormId(form.getId());
		result.setName(form.getName());
		result.setCaption(form.getCaption());
		result.setCreatedBy(UserSummary.from(form.getCreatedBy()));
		result.setCreationTime(form.getCreationTime());
		result.setModificationTime(form.getUpdateTime());
		return result;
	}
}
