package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.rest.RestErrorController;

public class AuthTokenFilter extends GenericFilterBean {
	private static final String OS_CLIENT_HDR = "X-OS-API-CLIENT";

	private static final String BASIC_AUTH = "Basic ";
	
	private static final String DEFAULT_AUTH_DOMAIN = "openspecimen";
	
	private UserAuthenticationService authService;
	
	private Map<String, List<String>> excludeUrls = new HashMap<>();
	
	private AuditService auditService;
	
	public UserAuthenticationService getAuthService() {
		return authService;
	}

	public void setAuthService(UserAuthenticationService authService) {
		this.authService = authService;
	}

	public Map<String, List<String>> getExcludeUrls() {
		return excludeUrls;
	}

	public void setExcludeUrls(Map<String, List<String>> excludeUrls) {
		this.excludeUrls = excludeUrls;
	}

	public void addExcludeUrl(String method, String resourceUrl) {
		List<String> urls = excludeUrls.computeIfAbsent(method, (key) -> new ArrayList<>());
		if (urls.indexOf(resourceUrl) == -1) {
			urls.add(resourceUrl);
		}
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException {
		if (!(req instanceof HttpServletRequest)) {
			throw new IllegalAccessError("Unknown protocol request");
		}

		try {
			doFilter0(req, resp, chain);
		} catch (Exception e) {
			sendError((HttpServletRequest) req, (HttpServletResponse) resp, e);
		}
	}

	private void doFilter0(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest)req;
		HttpServletResponse httpResp = (HttpServletResponse)resp;
		
		httpResp.setHeader("Access-Control-Allow-Origin", "http://localhost:9000");
		httpResp.setHeader("Access-Control-Allow-Credentials", "true");
		httpResp.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH, OPTIONS");
		httpResp.setHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, X-OS-API-TOKEN, X-OS-API-CLIENT, X-OS-IMPERSONATE-USER, X-OS-CLIENT-TZ");
		httpResp.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Length, Content-Type");

		httpResp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		httpResp.setHeader("Pragma", "no-cache");
		httpResp.setDateHeader("Expires", 0);

		if (httpReq.getMethod().equalsIgnoreCase("options")) {
			httpResp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		User user = null;
		String authToken = AuthUtil.getAuthTokenFromHeader(httpReq);
		if (authToken == null && !requiresAuthTokenHeader(httpReq)) {
			authToken = AuthUtil.getTokenFromCookie(httpReq);
		}
		
		LoginAuditLog loginAuditLog = null;
		if (authToken != null) {
			TokenDetail tokenDetail = new TokenDetail();
			tokenDetail.setToken(authToken);
			tokenDetail.setIpAddress(httpReq.getRemoteAddr());			

			ResponseEvent<AuthToken> atResp = authService.validateToken(new RequestEvent<>(tokenDetail));
			if (atResp.isSuccessful()) {
				user = atResp.getPayload().getUser();
				loginAuditLog = atResp.getPayload().getLoginAuditLog();
			}
		} else if(httpReq.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			AuthToken token = doBasicAuthentication(httpReq, httpResp);
			if (token != null) {
				user = token.getUser();
				loginAuditLog = token.getLoginAuditLog();
			}
		}
		
		if (user == null) {
			String clientHdr = httpReq.getHeader(OS_CLIENT_HDR);
			if (clientHdr != null && clientHdr.equals("webui")) {
				setUnauthorizedResp(req, resp, chain, false);
			} else {
				setRequireAuthResp(req, resp, chain);
			}
			return;
		}

		if (user.isContact() || user.isClosed()) {
			AuthUtil.clearTokenCookie(httpReq, httpResp);
			httpResp.sendError(
				HttpServletResponse.SC_UNAUTHORIZED,
				String.format(
					"%s (%d) is not allowed to sign-in. Type = %s, Status = %s",
					user.formattedName(), user.getId(), user.getType().name(), user.getActivityStatus()
				)
			);
			return;
		}

		User impersonatedUser = null;
		if (user.isAdmin()) {
			String impUserStr = AuthUtil.getImpersonateUser(httpReq);
			if (StringUtils.isNotBlank(impUserStr)) {
				impersonatedUser = getUser(impUserStr);
				if (impersonatedUser == null) {
					httpResp.sendError(HttpServletResponse.SC_BAD_REQUEST, "User " + impUserStr + " does not exist!");
					return;
				} else if (!impersonatedUser.isActive()) {
					httpResp.sendError(HttpServletResponse.SC_BAD_REQUEST, "User " + impUserStr + " is not active!");
					return;
				}
			}
		}

		AuthUtil.setCurrentUser(impersonatedUser != null ? impersonatedUser : user, authToken, httpReq);
		Date callStartTime = Calendar.getInstance().getTime();
		chain.doFilter(req, resp);
		AuthUtil.clearCurrentUser();

		if (isRecordableApi(httpReq)) {
			UserApiCallLog userAuditLog = new UserApiCallLog();
			userAuditLog.setUser(user);
			userAuditLog.setImpersonatedUser(impersonatedUser);
			userAuditLog.setUrl(httpReq.getRequestURI());
			userAuditLog.setMethod(httpReq.getMethod());
			userAuditLog.setCallStartTime(callStartTime);
			userAuditLog.setCallEndTime(Calendar.getInstance().getTime());
			userAuditLog.setResponseCode(Integer.toString(httpResp.getStatus()));
			userAuditLog.setLoginAuditLog(loginAuditLog);
			auditService.insertApiCallLog(userAuditLog);
		}
	}

	private AuthToken doBasicAuthentication(HttpServletRequest httpReq, HttpServletResponse httpResp) throws UnsupportedEncodingException {
		String header = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BASIC_AUTH)) {
			return null;
		}

		byte[] base64Token = header.substring(BASIC_AUTH.length()).getBytes();
		String[] parts = new String(Base64.decode(base64Token)).split(":");
		if (parts.length != 2) {
			return null;
		}

		LoginDetail detail = new LoginDetail();
		detail.setLoginName(parts[0]);
		detail.setPassword(parts[1]);
		detail.setIpAddress(httpReq.getRemoteAddr());
		detail.setDomainName(DEFAULT_AUTH_DOMAIN);
		detail.setDoNotGenerateToken(true);

		RequestEvent<LoginDetail> req = new RequestEvent<LoginDetail>(detail);
		ResponseEvent<Map<String, Object>> resp = authService.authenticateUser(req);
		if (resp.isSuccessful()) {
			return (AuthToken) resp.getPayload().get("tokenObj");
		}

		return null;
	}

	private void setRequireAuthResp(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException, ServletException {
		if (requiresSignIn(req)) {
			HttpServletResponse httpResp = (HttpServletResponse)resp;
			httpResp.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OpenSpecimen\"");
			setUnauthorizedResp(req, resp, chain, true);
		} else {
			chain.doFilter(req, resp);
		}
	}

	private void setUnauthorizedResp(ServletRequest req, ServletResponse resp, FilterChain chain, boolean reqSignIn)
	throws IOException, ServletException {
		if (reqSignIn || requiresSignIn(req)) {
			HttpServletResponse httpResp = (HttpServletResponse)resp;
			AuthUtil.clearTokenCookie((HttpServletRequest) req, httpResp);
			httpResp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must supply valid credentials to access the OpenSpecimen REST API");
		} else {
			chain.doFilter(req, resp);
		}
	}

	private boolean requiresSignIn(ServletRequest req) {
		HttpServletRequest httpReq = (HttpServletRequest)req;

		List<String> urls = excludeUrls.get(httpReq.getMethod());
		if (urls == null) {
			urls = Collections.emptyList();
		}

		boolean requiresSignIn = true;
		for (String url : urls) {
			if (matches(httpReq, url)) {
				requiresSignIn = false;
				break;
			}
		}

		return requiresSignIn;
	}

	private boolean requiresAuthTokenHeader(HttpServletRequest httpReq) {
		return !"GET".equalsIgnoreCase(httpReq.getMethod()) && !httpReq.getRequestURI().contains("rest/ng/de-forms");
	}

	private boolean isRecordableApi(HttpServletRequest httpReq) {
		return !matches(httpReq, "/user-notifications/unread-count/**");
	}

	private boolean matches(HttpServletRequest httpReq, String url) {
		return new AntPathRequestMatcher(getUrlPattern(url), httpReq.getMethod(), true).matches(httpReq);
	}

	private String getUrlPattern(String url) {
		if (!url.startsWith("/**")) {
			String prefix = "/**";
			if (!url.startsWith("/")) {
				prefix += "/";
			}

			url = prefix + url;
		}

		return url;
	}

	private void sendError(HttpServletRequest httpReq, HttpServletResponse httpResp, Exception e)
	throws IOException {
		ResponseEntity<Object> resp = new RestErrorController().handleOtherException(e, new ServletWebRequest(httpReq));

		httpResp.setContentType("application/json");
		httpResp.setStatus(resp.getStatusCodeValue());
		httpResp.getOutputStream().write(toJsonString(resp.getBody()).getBytes());
		httpResp.getOutputStream().flush();
		httpResp.getOutputStream().close();
	}

	private String toJsonString(Object body) {
		String unknownError = "[{\"message\": \"Unknown error\"}]";
		if (body == null) {
			return unknownError;
		}

		try {
			return new ObjectMapper().writeValueAsString(body);
		} catch (Exception e) {
			e.printStackTrace();
			return unknownError;
		}
	}

	private User getUser(String userString) {
		String[] parts = userString.split("/", 2);

		String domain = "openspecimen";
		String loginName = null;
		if (parts.length == 2) {
			domain = parts[0];
			loginName = parts[1];
		} else {
			loginName = parts[0];
		}

		return authService.getUser(domain, loginName);
	}
}