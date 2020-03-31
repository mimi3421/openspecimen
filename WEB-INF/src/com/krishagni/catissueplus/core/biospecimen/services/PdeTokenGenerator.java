package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.events.PdeTokenDetail;

public interface PdeTokenGenerator {
	PdeTokenDetail generate(CollectionProtocolRegistration cpr, Map<String, Object> detail);

	PdeTokenDetail getToken(CollectionProtocolRegistration cpr, Long tokenId);

	List<PdeTokenDetail> getTokens(List<Long> tokenIds);
}
