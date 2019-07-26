
package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserEvent;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.auth.AuthConfig;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.auth.services.AuthenticationService;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class UserAuthenticationServiceImpl implements UserAuthenticationService {
	private static final String ACCOUNT_LOCKED_NOTIF_TMPL = "account_locked_notification";

	private DaoFactory daoFactory;
	
	private AuditService auditService;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> authenticateUser(RequestEvent<LoginDetail> req) {
		LoginDetail loginDetail = req.getPayload();
		User user = null;
		try {
			user = daoFactory.getUserDao().getUser(loginDetail.getLoginName(), loginDetail.getDomainName());
			if (user == null) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}

			if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			}

			if (user.getActivityStatus().equals(Status.ACTIVITY_STATUS_LOCKED.getStatus())) {
				throw OpenSpecimenException.userError(AuthErrorCode.USER_LOCKED, loginDetail.getLoginName());
			}
			
			if (user.getActivityStatus().equals(Status.ACTIVITY_STATUS_EXPIRED.getStatus())) {
				throw OpenSpecimenException.userError(AuthErrorCode.PASSWD_EXPIRED);
			}
			
			if (!user.isEnabled()) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}
			
			AuthenticationService authService = user.getAuthDomain().getAuthProviderInstance();
			authService.authenticate(loginDetail);

			//
			// publish auth data for anyone to do extra checks
			//
			Map<String, Object> authEventData = new HashMap<>();
			authEventData.put("loginDetail", loginDetail);
			authEventData.put("user", user);
			EventPublisher.getInstance().publish(UserEvent.XTRA_AUTH, authEventData);

			Map<String, Object> authDetail = new HashMap<>();
			authDetail.put("user", user);

			AuthToken token = createToken(user, loginDetail);
			if (token != null) {
				authDetail.put("token", AuthUtil.encodeToken(token.getToken()));
				authDetail.put("tokenObj", token);
			}
			
			return ResponseEvent.response(authDetail);
		} catch (OpenSpecimenException ose) {
			if (user != null && user.isEnabled()) {
				insertLoginAudit(user, loginDetail.getIpAddress(), false);
				checkFailedLoginAttempt(user, loginDetail.getIpAddress());
			}
			return ResponseEvent.error(ose, true);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@PlusTransactional
	public ResponseEvent<AuthToken> validateToken(RequestEvent<TokenDetail> req) {
		try {
			TokenDetail tokenDetail = req.getPayload();
			String token = AuthUtil.decodeToken(tokenDetail.getToken());

			AuthToken authToken = daoFactory.getAuthDao().getAuthTokenByKey(token);
			if (authToken == null) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_TOKEN);
			}

			User user = authToken.getUser();
			long timeSinceLastApiCall = auditService.getTimeSinceLastApiCall(user.getId(), token);
			int tokenInactiveInterval = AuthConfig.getInstance().getTokenInactiveIntervalInMinutes();
			if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			}

			if (timeSinceLastApiCall > tokenInactiveInterval) {
				daoFactory.getAuthDao().deleteAuthToken(authToken);
				throw OpenSpecimenException.userError(AuthErrorCode.TOKEN_EXPIRED);
			}

			if (AuthConfig.getInstance().isTokenIpVerified()) {
				if (!tokenDetail.getIpAddress().equals(authToken.getIpAddress())) {
					throw OpenSpecimenException.userError(AuthErrorCode.IP_ADDRESS_CHANGED);
				}
			}

			if (!Hibernate.isInitialized(user)) {
				Hibernate.initialize(user);
			}
			
			return ResponseEvent.response(authToken);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@PlusTransactional
	public ResponseEvent<UserSummary> getCurrentLoggedInUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = (User)auth.getPrincipal();
		return ResponseEvent.response(UserSummary.from(user));
	}

	@PlusTransactional
	public ResponseEvent<String> removeToken(RequestEvent<String> req) {
		String userToken = AuthUtil.decodeToken(req.getPayload());
		try {
			AuthToken token = daoFactory.getAuthDao().getAuthTokenByKey(userToken);
			LoginAuditLog loginAuditLog = token.getLoginAuditLog();
			loginAuditLog.setLogoutTime(Calendar.getInstance().getTime());
			
			daoFactory.getAuthDao().deleteAuthToken(token);
			return ResponseEvent.response("Success");
		} catch (Exception e) {	
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public User getUser(String domainName, String loginName) {
		return daoFactory.getUserDao().getUser(loginName, domainName);
	}

	@Scheduled(cron="0 0 12 ? * *")
	@PlusTransactional
	public void deleteInactiveAuthTokens() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -AuthConfig.getInstance().getTokenInactiveIntervalInMinutes());
		daoFactory.getAuthDao().deleteInactiveAuthTokens(cal.getTime());
	}
	
	public String generateToken(User user, LoginDetail loginDetail) {
		AuthToken token = createToken(user, loginDetail);
		return token != null ? AuthUtil.encodeToken(token.getToken()) : null;
	}

	private AuthToken createToken(User user, LoginDetail loginDetail) {
		LoginAuditLog loginAuditLog = insertLoginAudit(user, loginDetail.getIpAddress(), true);

		AuthToken authToken = new AuthToken();
		authToken.setIpAddress(loginDetail.getIpAddress());
		authToken.setUser(user);
		authToken.setLoginAuditLog(loginAuditLog);

		if (loginDetail.isDoNotGenerateToken()) {
			return authToken;
		}

		String token = UUID.randomUUID().toString();
		authToken.setToken(token);
		daoFactory.getAuthDao().saveAuthToken(authToken);
		insertApiCallLog(loginDetail, user, loginAuditLog);
		return authToken;
	}

	private LoginAuditLog insertLoginAudit(User user, String ipAddress, boolean loginSuccessful) {
		LoginAuditLog loginAuditLog = new LoginAuditLog();
		loginAuditLog.setUser(user);
		loginAuditLog.setIpAddress(ipAddress);
		loginAuditLog.setLoginTime(Calendar.getInstance().getTime());
		loginAuditLog.setLogoutTime(null);
		loginAuditLog.setLoginSuccessful(loginSuccessful); 
		
		daoFactory.getAuthDao().saveLoginAuditLog(loginAuditLog);
		return loginAuditLog;
	}
	
	private void checkFailedLoginAttempt(User user, String ipAddress) {
		int failedLoginAttempts = AuthConfig.getInstance().getAllowedFailedLoginAttempts();
		List<LoginAuditLog> logs = daoFactory.getAuthDao()
			.getLoginAuditLogsByUser(user.getId(), failedLoginAttempts);
		
		if (logs.size() < failedLoginAttempts) {
			return;
		}
		
		for (LoginAuditLog log: logs) {
			if (log.isLoginSuccessful()) {
				return;
			}
		}
		
		user.setActivityStatus(Status.ACTIVITY_STATUS_LOCKED.getStatus());
		notifyUserAccountLocked(user, failedLoginAttempts);
	}
	
	private void insertApiCallLog(LoginDetail loginDetail, User user, LoginAuditLog loginAuditLog) {
		UserApiCallLog userAuditLog = new UserApiCallLog();
		userAuditLog.setUser(user);
		userAuditLog.setUrl(loginDetail.getApiUrl());
		userAuditLog.setMethod(loginDetail.getRequestMethod());
		userAuditLog.setResponseCode(Integer.toString(HttpStatus.OK.value()));
		userAuditLog.setCallStartTime(Calendar.getInstance().getTime());
		userAuditLog.setCallEndTime(Calendar.getInstance().getTime());
		userAuditLog.setLoginAuditLog(loginAuditLog);
		auditService.insertApiCallLog(userAuditLog);
	}

	private boolean isSystemLockedDown() {
		return ConfigUtil.getInstance().getBoolSetting("administrative", "system_lockdown", false);
	}

	private void notifyUserAccountLocked(User lockedUser, int failedLoginAttempts) {
		String[] subjParams = {lockedUser.getFirstName(), lockedUser.getLastName()};

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("lockedUser", lockedUser);
		emailProps.put("failedLoginAttempts", failedLoginAttempts);
		emailProps.put("$subject", subjParams);
		emailProps.put("ccAdmin", false);

		List<User> rcpts = daoFactory.getUserDao().getSuperAndInstituteAdmins(lockedUser.getInstitute().getName());
		if (!rcpts.contains(lockedUser)) {
			rcpts.add(lockedUser);
		}

		for (User rcpt : rcpts) {
			emailProps.put("user", rcpt);
			EmailUtil.getInstance().sendEmail(ACCOUNT_LOCKED_NOTIF_TMPL, new String[] {rcpt.getEmailAddress()}, null, emailProps);
		}


		//
		// remove the user who is locked as they can't see the UI notification
		//
		rcpts.remove(lockedUser);

		Notification notif = new Notification();
		notif.setEntityId(lockedUser.getId());
		notif.setEntityType(User.getEntityName());
		notif.setCreatedBy(daoFactory.getUserDao().getSystemUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setOperation("UPDATE");
		notif.setMessage(MessageUtil.getInstance().getMessage(ACCOUNT_LOCKED_NOTIF_TMPL + "_subj", subjParams));
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("user-overview", rcpts));
	}
}
