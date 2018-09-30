
package com.krishagni.catissueplus.core.common.events;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonFilter("withoutId")
public class UserSummary implements Serializable {
	
	private static final long serialVersionUID = -8113791999197573026L;

	private Long id;

	private String firstName;

	private String lastName;

	private String loginName;
	
	private String domain;
	
	private String emailAddress;

	private String instituteName;

	private String primarySite;

	private Boolean admin;
	
	private Boolean instituteAdmin;
	
	private Boolean manageForms;

	private int cpCount;

	private Date creationDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public String getPrimarySite() {
		return primarySite;
	}

	public void setPrimarySite(String primarySite) {
		this.primarySite = primarySite;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}
	
	public Boolean getInstituteAdmin() {
		return instituteAdmin;
	}
	
	public void setInstituteAdmin(Boolean instituteAdmin) {
		this.instituteAdmin = instituteAdmin;
	}

	public Boolean getManageForms() {
		return manageForms;
	}

	public void setManageForms(Boolean manageForms) {
		this.manageForms = manageForms;
	}

	public int getCpCount() {
		return cpCount;
	}

	public void setCpCount(int cpCount) {
		this.cpCount = cpCount;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String formattedName() {
		StringBuilder name = new StringBuilder();
		if (StringUtils.isNotBlank(firstName)) {
			name.append(firstName);
		}

		if (StringUtils.isNotBlank(lastName)) {
			if (name.length() > 0) {
				name.append(" ");
			}

			name.append(lastName);
		}

		return name.toString();
	}

	public static UserSummary from(User user) {
		UserSummary userSummary = new UserSummary();
		userSummary.setId(user.getId());
		userSummary.setFirstName(user.getFirstName());
		userSummary.setLastName(user.getLastName());
		userSummary.setLoginName(user.getLoginName());
		userSummary.setDomain(user.getAuthDomain().getName());
		userSummary.setEmailAddress(user.getEmailAddress());
		userSummary.setAdmin(user.isAdmin());
		userSummary.setInstituteAdmin(user.isInstituteAdmin());
		userSummary.setCreationDate(user.getCreationDate());
		userSummary.setManageForms(user.getManageForms());

		if (user.getInstitute() != null) {
			userSummary.setInstituteName(user.getInstitute().getName());
		}

		if (user.getPrimarySite() != null) {
			userSummary.setPrimarySite(user.getPrimarySite().getName());
		}

		return userSummary;
	}
	
	public static List<UserSummary> from(Collection<User> users) {
		return Utility.nullSafeStream(users).map(UserSummary::from).collect(Collectors.toList());
	}
}
