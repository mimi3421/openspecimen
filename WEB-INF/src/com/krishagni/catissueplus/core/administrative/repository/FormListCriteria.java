package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class FormListCriteria extends AbstractListCriteria<FormListCriteria> {
	private List<String> entityTypes;

	private Long userId;

	private Long cpId;

	private Boolean excludeSysForm;

	private Set<SiteCpPair> siteCps;
	
	@Override
	public FormListCriteria self() {
		return this;
	}

	public List<String> entityTypes() {
		return entityTypes;
	}

	public FormListCriteria entityTypes(List<String> entityTypes) {
		this.entityTypes = entityTypes;
		return self();
	}

	public Long userId() {
		return userId;
	}

	public FormListCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Long cpId() {
		return cpId;
	}

	public FormListCriteria cpId(Long cpId) {
		this.cpId = cpId;
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
