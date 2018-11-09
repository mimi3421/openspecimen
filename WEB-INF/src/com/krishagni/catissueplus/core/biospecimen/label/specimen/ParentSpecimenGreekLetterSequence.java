package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractLetterSequenceToken;

public class ParentSpecimenGreekLetterSequence extends AbstractLetterSequenceToken<Specimen> {
//	private static final String ALPHABET = "αβγδεζηθικλμνξοπστυφχψω";
	private static final String ALPHABET = "\u03b1\u03b2\u03b3\u03b4\u03b5\u03b6\u03b7\u03b8\u03b9\u03ba\u03bb\u03bc\u03bd\u03be\u03bf\u03c0\u03c1\u03c3\u03c2\u03c4\u03c5\u03c6\u03c7\u03c8\u03c9";

	@Autowired
	private DaoFactory daoFactory;

	public ParentSpecimenGreekLetterSequence() {
		this.name = "PSPEC_GREEK_SEQ";
	}

	@Override
	protected Integer getSequence(Specimen specimen) {
		if (specimen.getParentSpecimen() == null) {
			return null;
		}

		String pidStr = specimen.getParentSpecimen().getId().toString();
		Long count = daoFactory.getUniqueIdGenerator().getUniqueId(name, pidStr);
		return count.intValue();
	}

	@Override
	protected String getAlphabet() {
		return ALPHABET;
	}
}