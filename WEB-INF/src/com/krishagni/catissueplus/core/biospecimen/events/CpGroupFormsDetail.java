package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.events.FormSummary;

public class CpGroupFormsDetail {
	private Long groupId;

	private String groupName;

	private String level;

	private List<FormSummary> forms;

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<FormSummary> getForms() {
		return forms;
	}

	public void setForms(List<FormSummary> forms) {
		this.forms = forms;
	}

	public static CpGroupFormsDetail from(CollectionProtocolGroup group, String level, Collection<Form> forms) {
		CpGroupFormsDetail result = new CpGroupFormsDetail();
		result.setGroupId(group.getId());
		result.setGroupName(group.getName());
		result.setLevel(level);
		result.setForms(forms.stream().map(FormSummary::from).collect(Collectors.toList()));
		return result;
	}
}
