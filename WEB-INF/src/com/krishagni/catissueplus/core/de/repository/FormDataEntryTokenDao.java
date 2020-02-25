package com.krishagni.catissueplus.core.de.repository;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.FormDataEntryToken;

public interface FormDataEntryTokenDao extends Dao<FormDataEntryToken> {
	FormDataEntryToken getByToken(String token);

	int deleteOldTokens();
}
