package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class FormListCriteria extends AbstractListCriteria<FormListCriteria> {
	private String formType;

	private Long userId;

	private Boolean excludeSysForm;

	private Set<SiteCpPair> siteCps;
	
	@Override
	public FormListCriteria self() {
		return this;
	}

	public String getFormType() {
		return formType;
	}

	public FormListCriteria formType(String formType) {
		this.formType = formType;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public FormListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Boolean excludeSysForm() {
		return excludeSysForm;
	}

	public FormListCriteria excludeSysForm(Boolean excludeSysForm) {
		this.excludeSysForm = excludeSysForm;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public FormListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}
}
