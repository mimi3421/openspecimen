package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.DataEntryToken;

public interface PdeTokenGenerator {
	DataEntryToken generate(CollectionProtocolRegistration cpr, Map<String, Object> detail);

	DataEntryToken getToken(CollectionProtocolRegistration cpr, Long tokenId);
}
