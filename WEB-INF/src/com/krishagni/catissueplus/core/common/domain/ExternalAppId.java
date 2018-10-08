package com.krishagni.catissueplus.core.common.domain;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class ExternalAppId extends BaseEntity  {
	private String appName;

	private String entityName;

	private String externalId;

	private Long osId;

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public Long getOsId() {
		return osId;
	}

	public void setOsId(Long osId) {
		this.osId = osId;
	}

	public String toString() {
		return "id = " + getId() + ", "
			+ "app name = " + getAppName() + ", "
			+ "entity name = " + getEntityName() + ", "
			+ "external ID = " + getExternalId() + ", "
			+ "OS ID = " + getOsId();
	}
}
