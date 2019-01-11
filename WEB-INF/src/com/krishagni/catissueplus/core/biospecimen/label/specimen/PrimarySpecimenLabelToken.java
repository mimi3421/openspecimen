package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

public class PrimarySpecimenLabelToken extends AbstractSpecimenLabelToken {
	public PrimarySpecimenLabelToken() {
		this.name = "PR_SPEC_LABEL";
	}

	@Override
	public String getLabel(Specimen specimen) {
		String label = specimen.getLabel();
		while (specimen.getParentSpecimen() != null) {
			specimen = specimen.getParentSpecimen();
			label = specimen.getLabel();
		}

		return label;
	}
}
