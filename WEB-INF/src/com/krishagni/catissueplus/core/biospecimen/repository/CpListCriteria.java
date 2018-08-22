
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class CpListCriteria extends AbstractListCriteria<CpListCriteria> {
	
	private String title;
	
	private Long piId;
	
	private String repositoryName;
	
	private boolean includePi;

	private Set<SiteCpPair> siteCps;
	
	@Override
	public CpListCriteria self() {
		return this;
	}
	
	public String title() {
		return this.title;
	}
	
	public CpListCriteria title(String title) {
		this.title = title;
		return self();
	}
	
	public Long piId() {
		return this.piId;
	}
	
	public CpListCriteria piId(Long piId) {
		this.piId = piId;
		return self();
	}
	
	public String repositoryName() {
		return this.repositoryName;
	}
	
	public CpListCriteria repositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
		return self();
	}
	
	public boolean includePi() {
		return includePi;
	}
	
	public CpListCriteria includePi(boolean includePi) {
		this.includePi = includePi;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public CpListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}
}
