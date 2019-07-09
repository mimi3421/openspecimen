package com.krishagni.catissueplus.core.biospecimen.domain;


import com.krishagni.catissueplus.core.de.domain.Form;

public class CpGroupForm extends BaseEntity {
	private CollectionProtocolGroup group;

	private String level;

	private Form form;

	private boolean multipleRecords;

	public CollectionProtocolGroup getGroup() {
		return group;
	}

	public void setGroup(CollectionProtocolGroup group) {
		this.group = group;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public boolean isMultipleRecords() {
		return multipleRecords;
	}

	public void setMultipleRecords(boolean multipleRecords) {
		this.multipleRecords = multipleRecords;
	}
}
