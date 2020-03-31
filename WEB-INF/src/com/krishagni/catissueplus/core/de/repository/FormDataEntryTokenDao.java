package com.krishagni.catissueplus.core.de.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.PdeTokenDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.FormDataEntryToken;

public interface FormDataEntryTokenDao extends Dao<FormDataEntryToken> {
	FormDataEntryToken getByToken(String token);

	List<PdeTokenDetail> getTokens(List<Long> tokenIds);
}
