package com.krishagni.catissueplus.core.administrative.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

import java.util.Date;
import java.util.List;

public class UserListCriteria extends AbstractListCriteria<UserListCriteria> {
	private static final String ARCHIVED = "Archived";

	private String name;
	
	private String loginName;
	
	private String activityStatus;
	
	private String instituteName;
	
	private String domainName;
	
	private boolean listAll;

	private String type;

	private List<String> excludeTypes;

	private Date activeSince;

	private String siteName;

	private String cpShortTitle;

	private List<String> roleNames;

	private String resourceName;

	private List<String> opNames;
	
	@Override
	public UserListCriteria self() {
		return this;
	}

	public String name() {
		return name;
	}

	public UserListCriteria name(String name) {
		this.name = name;
		return self();
	}

	public String loginName() {
		return loginName;
	}

	public UserListCriteria loginName(String loginName) {
		this.loginName = loginName;
		return self();
	}

	public String activityStatus() {
		return activityStatus;
	}

	public UserListCriteria activityStatus(String activityStatus) {
		if (ARCHIVED.equalsIgnoreCase(activityStatus)) {
			activityStatus = "Closed";
		}

		this.activityStatus = activityStatus;
		return self();
	}
	
	public UserListCriteria instituteName(String instituteName) {
		this.instituteName = instituteName;
		return self();
	}
	
	public String instituteName() {
		return instituteName;
	}	
	
	public UserListCriteria domainName(String domainName) {
		this.domainName = domainName;
		return self();
	}
	
	public String domainName() {
		return domainName;
	}
	
	public boolean listAll() {
		return listAll;
	}
	
	public UserListCriteria listAll(boolean listAll) {
		this.listAll = listAll;
		return self();
	}

	public String type() {
		return type;
	}

	public UserListCriteria type(String type) {
		this.type = type;
		return self();
	}

	public List<String> excludeTypes() {
		return excludeTypes;
	}

	public UserListCriteria excludeTypes(List<String> excludeTypes) {
		this.excludeTypes = excludeTypes;
		return self();
	}

	public Date activeSince() {
		return activeSince;
	}

	public UserListCriteria activeSince(Date activeSince) {
		this.activeSince = activeSince;
		return self();
	}

	public String siteName() {
		return siteName;
	}

	public UserListCriteria siteName(String siteName) {
		this.siteName = siteName;
		return self();
	}

	public String cpShortTitle() {
		return cpShortTitle;
	}

	public UserListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public List<String> roleNames() {
		return roleNames;
	}

	public UserListCriteria roleNames(List<String> roleNames) {
		this.roleNames = roleNames;
		return self();
	}

	public String resourceName() {
		return resourceName;
	}

	public UserListCriteria resourceName(String resourceName) {
		this.resourceName = resourceName;
		return self();
	}

	public List<String> opNames() {
		return opNames;
	}

	public UserListCriteria opNames(List<String> opNames) {
		this.opNames = opNames;
		return self();
	}
}
