package com.krishagni.catissueplus.core.query;

import java.util.Map;

public class Row {
	private Object[] data;

	private Map<String, Object> hidden;

	private Object[] appData;

	public Object[] getData() {
		return data;
	}

	public void setData(Object[] data) {
		this.data = data;
	}

	public Map<String, Object> getHidden() {
		return hidden;
	}

	public void setHidden(Map<String, Object> hidden) {
		this.hidden = hidden;
	}

	public Object[] getAppData() {
		return appData;
	}

	public void setAppData(Object[] appData) {
		this.appData = appData;
	}
}
