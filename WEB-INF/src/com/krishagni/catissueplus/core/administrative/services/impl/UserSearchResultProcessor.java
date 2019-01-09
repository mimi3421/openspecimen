package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class UserSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return User.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		UserListCriteria crit = new UserListCriteria().ids(entityIds);
		if (!AuthUtil.isAdmin()) {
			crit.instituteName(AuthUtil.getCurrentUserInstitute().getName());
		}

		List<User> users = daoFactory.getUserDao().getUsers(crit);
		return users.stream().collect(Collectors.toMap(User::getId, this::getProps));
	}

	private Map<String, Object> getProps(User user) {
		Map<String, Object> props = new HashMap<>();
		props.put("firstName", user.getFirstName());
		props.put("lastName", user.getLastName());
		props.put("loginName", user.getLoginName());
		props.put("emailAddress", user.getEmailAddress());
		props.put("displayName", user.formattedName());
		return props;
	}
}
