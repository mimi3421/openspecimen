
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.Password;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface UserDao extends Dao<User> {
	public List<User> getUsers(UserListCriteria criteria);
	
	public Long getUsersCount(UserListCriteria criteria);

	public List<User> getUsersByIds(Collection<Long> userIds);
	
	public List<User> getUsersByIdsAndInstitute(Collection<Long> userIds, Long instituteId);
	
	public User getUser(String loginName, String domain);

	List<User> getUsers(Collection<String> loginNames, String domain);
	
	public User getSystemUser();
	
	public User getUserByEmailAddress(String emailAddress);
	
	public Boolean isUniqueLoginName(String loginName, String domainName);
	
	public Boolean isUniqueEmailAddress(String emailAddress);
	
	public List<DependentEntityDetail> getDependentEntities(Long userId);
	
	public ForgotPasswordToken getFpToken(String token);
	
	public ForgotPasswordToken getFpTokenByUser(Long userId);
	
	public void saveFpToken(ForgotPasswordToken token);
	
	public void deleteFpToken(ForgotPasswordToken token);

	public List<String> getActiveUsersEmailIds(Date startDate, Date endDate);
	
	public List<Password> getPasswordsUpdatedBefore(Date updateDate);
	
	public List<User> getInactiveUsers(Date lastLoginTime);
	
	public int updateStatus(List<User> users, String status);

	List<User> getSuperAndInstituteAdmins(String instituteName);
}
