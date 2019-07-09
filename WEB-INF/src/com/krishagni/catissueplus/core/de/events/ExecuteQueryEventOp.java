package com.krishagni.catissueplus.core.de.events;


import org.apache.commons.lang3.StringUtils;

public class ExecuteQueryEventOp  {
	
	private Long cpId;

	private Long cpGroupId;
	
	private String drivingForm;

	private String aql;
	
	private String wideRowMode = "OFF";
	
	private Long savedQueryId;
	
	private String runType = "Data";
	
	private String indexOf;

	private boolean outputIsoDateTime;

	private boolean outputColumnExprs;

	private boolean synchronous;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getCpGroupId() {
		return cpGroupId;
	}

	public void setCpGroupId(Long cpGroupId) {
		this.cpGroupId = cpGroupId;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	public String getAql() {
		return aql;
	}

	public void setAql(String aql) {
		this.aql = aql;
	}

	public String getWideRowMode() {
		return wideRowMode;
	}

	public void setWideRowMode(String wideRowMode) {
		this.wideRowMode = wideRowMode;
	}

	public Long getSavedQueryId() {
		return savedQueryId;
	}

	public void setSavedQueryId(Long savedQueryId) {
		this.savedQueryId = savedQueryId;
	}

	public String getRunType() {
		return StringUtils.isBlank(runType) ? "Data" : runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public String getIndexOf() {
		return indexOf;
	}

	public void setIndexOf(String indexOf) {
		this.indexOf = indexOf;
	}

	public boolean isOutputIsoDateTime() {
		return outputIsoDateTime;
	}

	public void setOutputIsoDateTime(boolean outputIsoDateTime) {
		this.outputIsoDateTime = outputIsoDateTime;
	}

	public boolean isOutputColumnExprs() {
		return outputColumnExprs;
	}

	public void setOutputColumnExprs(boolean outputColumnExprs) {
		this.outputColumnExprs = outputColumnExprs;
	}

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}
}
