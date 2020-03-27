package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.List;
import java.util.Map;

public class PdeTokenGenDetail {
	private String cpShortTitle;

	private List<String> ppids;

	private List<Map<String, Object>> forms;

	private boolean notifyByEmail;

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public List<String> getPpids() {
		return ppids;
	}

	public void setPpids(List<String> ppids) {
		this.ppids = ppids;
	}

	public List<Map<String, Object>> getForms() {
		return forms;
	}

	public void setForms(List<Map<String, Object>> forms) {
		this.forms = forms;
	}

	public boolean isNotifyByEmail() {
		return notifyByEmail;
	}

	public void setNotifyByEmail(boolean notifyByEmail) {
		this.notifyByEmail = notifyByEmail;
	}
}
