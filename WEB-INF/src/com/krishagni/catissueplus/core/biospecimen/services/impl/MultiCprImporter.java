package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class MultiCprImporter implements ObjectImporter<ParticipantRegistrationsList, ParticipantRegistrationsList> {

	private CollectionProtocolRegistrationService cprSvc;

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	@Override
	public ResponseEvent<ParticipantRegistrationsList> importObject(RequestEvent<ImportObjectDetail<ParticipantRegistrationsList>> req) {
		try {
			ImportObjectDetail<ParticipantRegistrationsList> detail = req.getPayload();

			ParticipantRegistrationsList regsList = detail.getObject();
			regsList.setForceDelete(true);

			ParticipantDetail participant = regsList.getParticipant();
			if (participant == null) {
				participant = new ParticipantDetail();
				regsList.setParticipant(participant);
			}

			if (StringUtils.isBlank(participant.getSource())) {
				participant.setSource(Participant.DEF_SOURCE);
			}

			if (detail.isCreate()) {
				if (regsList.getRegistrations() == null || regsList.getRegistrations().isEmpty()) {
					return ResponseEvent.userError(CprErrorCode.CP_REQUIRED);
				}

				return cprSvc.createRegistrations(new RequestEvent<>(regsList));
			} else {
				return cprSvc.updateRegistrations(new RequestEvent<>(regsList));
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
