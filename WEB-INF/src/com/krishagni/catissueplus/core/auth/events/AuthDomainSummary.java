package com.krishagni.catissueplus.core.auth.events;

import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;

public class AuthDomainSummary {
	private Long id;
	
	private String name;

	private String type;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static AuthDomainSummary from(AuthDomain domain) {
		AuthDomainSummary summary = new AuthDomainSummary();
		summary.setId(domain.getId());
		summary.setName(domain.getName());
		summary.setType(domain.getAuthProvider().getAuthType());
		return summary;
	}
	
	public static List<AuthDomainSummary> from(List<AuthDomain> domains) {
		return domains.stream().map(AuthDomainSummary::from).collect(Collectors.toList());
	}
}
