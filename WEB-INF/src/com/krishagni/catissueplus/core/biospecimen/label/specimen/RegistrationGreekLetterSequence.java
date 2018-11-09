package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractGreekLetterSequenceToken;

public class RegistrationGreekLetterSequence extends AbstractGreekLetterSequenceToken<Specimen> {

	public RegistrationGreekLetterSequence() {
		this.name = "REG_GREEK_SEQ";
	}

	@Override
	protected Integer getSequence(Specimen spmn) {
		return getUniqueId(spmn.getRegistration().getId().toString());
	}
}
