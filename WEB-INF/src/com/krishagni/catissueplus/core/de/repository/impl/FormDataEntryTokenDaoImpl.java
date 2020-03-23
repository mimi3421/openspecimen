package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.Calendar;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.de.domain.FormDataEntryToken;
import com.krishagni.catissueplus.core.de.repository.FormDataEntryTokenDao;

public class FormDataEntryTokenDaoImpl extends AbstractDao<FormDataEntryToken> implements FormDataEntryTokenDao {

	@Override
	public Class<FormDataEntryToken> getType() {
		return FormDataEntryToken.class;
	}

	@Override
	public FormDataEntryToken getByToken(String token) {
		return (FormDataEntryToken) getCurrentSession().getNamedQuery(GET_BY_TOKEN)
			.setParameter("token", token)
			.uniqueResult();
	}

	@Override
	public int deleteOldTokens() {
		return getCurrentSession().getNamedQuery(DELETE_EXPIRED_TOKENS)
			.setParameter("expireTime", Calendar.getInstance().getTime())
			.executeUpdate();
	}

	private static final String FQN = FormDataEntryToken.class.getName();

	private static final String GET_BY_TOKEN = FQN + ".getByToken";

	private static final String DELETE_EXPIRED_TOKENS = FQN + ".deleteExpiredTokens";
}