package com.krishagni.catissueplus.core.biospecimen.print;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.AbstractCustomFieldLabelToken;

public class CustomFieldPrintToken extends AbstractCustomFieldLabelToken {

	@Override
	public String getName() {
		return "custom_field";
	}

	@Override
	protected BaseExtensionEntity getObject(Object object, String level) {
		if (object instanceof StorageContainer) {
			return (StorageContainer) object;
		} else if (object instanceof Visit) {
			Visit visit = (Visit) object;
			switch (level) {
				case "visit":
					return visit;
				case "cpr":
					Participant participant = visit.getRegistration().getParticipant();
					participant.setCpId(visit.getCpId());
					return participant;
				case "cp":
					return visit.getCollectionProtocol();
			}
		} else if (object instanceof Specimen) {
			Specimen specimen = (Specimen) object;
			switch (level) {
				case "specimen":
					return specimen;
				case "parentSpecimen":
					return specimen.getParentSpecimen();
				case "visit":
					return specimen.getVisit();
				case "cpr":
					Participant participant = specimen.getRegistration().getParticipant();
					participant.setCpId(specimen.getCpId());
					return participant;
				case "cp":
					return specimen.getCollectionProtocol();
			}
		}

		return null;
	}
}
