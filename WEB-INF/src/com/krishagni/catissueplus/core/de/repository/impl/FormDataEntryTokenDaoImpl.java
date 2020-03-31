package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.biospecimen.events.PdeTokenDetail;
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
	public List<PdeTokenDetail> getTokens(List<Long> tokenIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_TOKENS_BY_ID)
			.setParameterList("tokenIds", tokenIds)
			.list();

		List<PdeTokenDetail> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			PdeTokenDetail token = new PdeTokenDetail();
			token.setCpShortTitle((String) row[++idx]);
			token.setPpid((String) row[++idx]);
			token.setCprId((Long) row[++idx]);
			token.setFormCaption((String) row[++idx]);
			token.setTokenId((Long) row[++idx]);
			token.setToken((String) row[++idx]);
			token.setCreationTime((Date) row[++idx]);
			token.setExpiryTime((Date) row[++idx]);
			token.setCompletionTime((Date) row[++idx]);
			result.add(token);
		}

		return result;
	}

	private static final String FQN = FormDataEntryToken.class.getName();

	private static final String GET_BY_TOKEN = FQN + ".getByToken";

	private static final String GET_TOKENS_BY_ID = FQN + ".getTokensById";
}
