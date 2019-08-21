
package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;

@Configurable
@Audited
public class User extends BaseEntity implements UserDetails {
	public enum Type {
		CONTACT,
		SUPER,
		INSTITUTE,
		NONE
	};

	public static final String SYS_USER = "$system";
	
	public static final String DEFAULT_AUTH_DOMAIN = "openspecimen";
	
	private static final long serialVersionUID = 1L;
	
	private static final String ENTITY_NAME = "user";

	private String lastName;

	private String firstName;

	private AuthDomain authDomain;

	private String emailAddress;

	private String phoneNumber;

	private String loginName;

	private Date creationDate;

	private String activityStatus;

	private Institute institute;

	private Site primarySite;

	private String address;

	private String comments;
	
	private String password;

	private Type type;

	private Boolean manageForms;

	private String timeZone;
	
	private Set<Password> passwords = new HashSet<>();

	private Set<SubjectRole> roles = new HashSet<>();

	private Set<SubjectAccess> acl = new HashSet<>();
	
	@Autowired 
	private DaoFactory daoFactory;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@NotAudited
	public AuthDomain getAuthDomain() {
		return authDomain;
	}

	public void setAuthDomain(AuthDomain authDomain) {
		this.authDomain = authDomain;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date createDate) {
		this.creationDate = createDate;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Institute getInstitute() {
		return institute;
	}
	
	public void setInstitute(Institute institute) {
		this.institute = institute;
	}

	public Site getPrimarySite() {
		return primarySite;
	}

	public void setPrimarySite(Site primarySite) {
		this.primarySite = primarySite;
	}

	@NotAudited
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	@NotAudited
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isContact() {
		return Type.CONTACT == getType();
	}

	public boolean isAdmin() {
		return Type.SUPER == getType();
	}
	
	public boolean isInstituteAdmin() {
		return Type.INSTITUTE == getType();
	}
	
	public boolean canManageForms() {
		return manageForms != null ? manageForms : false;
	}

	public Boolean getManageForms() {
		return manageForms;
	}

	public void setManageForms(Boolean manageForms) {
		this.manageForms = manageForms;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@NotAudited
	public Set<Password> getPasswords() {
		return passwords;
	}

	public void setPasswords(Set<Password> passwords) {
		this.passwords = passwords;
	}

	@NotAudited
	public Set<SubjectRole> getRoles() {
		return roles;
	}

	public void setRoles(Set<SubjectRole> roles) {
		this.roles = roles;
	}

	@NotAudited
	public Set<SubjectAccess> getAcl() {
		return acl;
	}

	public void setAcl(Set<SubjectAccess> acl) {
		this.acl = acl;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("user"));
		return authorities;
	}

	@Override
	public String getUsername() {
		return loginName;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !isExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return !isLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return isAccountNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return this.activityStatus.equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}

	public void update(User user) {
		setType(user.getType());

		updateStatus(user.getActivityStatus());
		if (isDisabled()) {
			return;
		}

		setFirstName(user.getFirstName());
		setLastName(user.getLastName());
		setAddress(user.getAddress());
		setInstitute(user.getInstitute());
		setPrimarySite(user.getPrimarySite());
		setEmailAddress(user.getEmailAddress());
		setPhoneNumber(user.getPhoneNumber());
		setComments(user.getComments());
		setTimeZone(user.getTimeZone());

		if (!isContact()) {
			setAuthDomain(user.getAuthDomain());
			setLoginName(user.getLoginName());
			setManageForms(user.canManageForms());
		}
	}

	public void changePassword(String newPassword) {
		if (isContact()) {
			return;
		}

		if (StringUtils.isBlank(newPassword) || !isValidPasswordPattern(newPassword)) {
			throw OpenSpecimenException.userError(UserErrorCode.PASSWD_VIOLATES_RULES);
		}
		
		int passwordsToExamine = ConfigUtil.getInstance().getIntSetting("auth", "passwords_to_examine", 0);
		if (isSameAsLastNPassword(newPassword, passwordsToExamine)) {
			throw OpenSpecimenException.userError(UserErrorCode.PASSWD_SAME_AS_LAST_N, passwordsToExamine);
		}

		setPassword(passwordEncoder.encode(newPassword));

		Password password = new Password();
		password.setUpdationDate(new Date());
		password.setUser(this);
		password.setPassword(getPassword());
		getPasswords().add(password);
		if (isExpired()) {
			setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		}
	}
	
	//TODO: need to check few more entities like AQ, Custom form, Distribution order etc.
	public List<DependentEntityDetail> getDependentEntities() {
		return daoFactory.getUserDao().getDependentEntities(getId());
	}

	public void close() {
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
	}

	public void delete() {
		List<DependentEntityDetail> dependentEntities = getDependentEntities();
		if (!dependentEntities.isEmpty()) {
			throw OpenSpecimenException.userError(UserErrorCode.REF_ENTITY_FOUND, formattedName());
		}

		setLoginName(Utility.getDisabledValue(getLoginName(), 255));
		setEmailAddress(Utility.getDisabledValue(getEmailAddress(), 255));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public boolean isValidOldPassword(String oldPassword) {
		if (StringUtils.isBlank(oldPassword)) {
			throw OpenSpecimenException.userError(UserErrorCode.OLD_PASSWD_NOT_SPECIFIED);
		}
		
		return passwordEncoder.matches(oldPassword, this.getPassword());
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

	public boolean isSysUser() {
		return SYS_USER.equals(getLoginName());
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isExpired() {
		return Status.ACTIVITY_STATUS_EXPIRED.getStatus().equals(getActivityStatus());
	}
	
	public boolean isLocked() {
		return Status.ACTIVITY_STATUS_LOCKED.getStatus().equals(getActivityStatus());
	}

	public boolean isPending() {
		return Status.ACTIVITY_STATUS_PENDING.getStatus().equals(getActivityStatus());
	}

	public boolean isClosed() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}

	public boolean isDisabled() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	private boolean isValidPasswordPattern(String password) {
		if (StringUtils.isBlank(password)) {
			return false;
		}

		String pattern = ConfigUtil.getInstance().getStrSetting("auth", "password_pattern", "");
		if (StringUtils.isBlank(pattern)) {
			return true;
		}

		return Pattern.compile(pattern).matcher(password).matches();
	}
	
	private boolean isSameAsLastNPassword(String newPassword, int passwordToExamine) {
		if (passwordToExamine <= 0) {
			return false;
		}

		List<Password> passwords = new ArrayList<>(this.getPasswords());
		Collections.sort(passwords);
		
		int examined = 0;
		boolean isSameAsLastN = false;
		for (Password passwd : passwords) {
			if (examined == passwordToExamine) {
				break;
			}
			
			if(passwordEncoder.matches(newPassword, passwd.getPassword())) {
				isSameAsLastN = true;
				break;
			}
			++examined;
		}

		return isSameAsLastN;
	}

	private void updateStatus(String activityStatus) {
		if (getActivityStatus().equals(activityStatus)) {
			return;
		}

		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus)) {
			delete();
		} else {
			setActivityStatus(activityStatus);
		}
	}
	
}
