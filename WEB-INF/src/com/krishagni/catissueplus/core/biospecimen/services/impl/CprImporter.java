package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class CprImporter implements ObjectImporter<CollectionProtocolRegistrationDetail, CollectionProtocolRegistrationDetail> {
	
	private CollectionProtocolRegistrationService cprSvc;
	
	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	@Override
	public ResponseEvent<CollectionProtocolRegistrationDetail> importObject(RequestEvent<ImportObjectDetail<CollectionProtocolRegistrationDetail>> req) {
		try {
			ImportObjectDetail<CollectionProtocolRegistrationDetail> detail = req.getPayload();

			CollectionProtocolRegistrationDetail cpr = detail.getObject();
			cpr.setForceDelete(true);

			ParticipantDetail participant = cpr.getParticipant();
			if (participant == null) {
				participant = new ParticipantDetail();
				cpr.setParticipant(participant);
			}

			if (StringUtils.isBlank(participant.getSource())) {
				participant.setSource(Participant.DEF_SOURCE);
			}

			if (detail.isCreate()) {
				return cprSvc.createRegistration(new RequestEvent<>(cpr));
			} else {
				return cprSvc.updateRegistration(new RequestEvent<>(cpr));
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
