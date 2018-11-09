package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractGreekLetterSequenceToken;

public class VisitGreekLetterSequence extends AbstractGreekLetterSequenceToken<Specimen> {

	public VisitGreekLetterSequence() {
		this.name = "VISIT_GREEK_SEQ";
	}

	@Override
	protected Integer getSequence(Specimen spmn) {
		return getUniqueId(spmn.getVisit().getId().toString());
	}
}
