package com.krishagni.catissueplus.core.common.repository.impl;

import com.krishagni.catissueplus.core.common.domain.ExternalAppId;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.ExternalAppIdDao;

public class ExternalAppIdDaoImpl extends AbstractDao<ExternalAppId> implements ExternalAppIdDao {
	@Override
	public ExternalAppId getByExternalId(String appName, String entityName, String externalId) {
		return (ExternalAppId) getCurrentSession().getNamedQuery(GET_BY_EXT_ID)
			.setParameter("appName", appName)
			.setParameter("entityName", entityName)
			.setParameter("extId", externalId)
			.uniqueResult();
	}

	private static final String FQN = ExternalAppId.class.getName();

	private static final String GET_BY_EXT_ID = FQN + ".getByExternalId";
}
