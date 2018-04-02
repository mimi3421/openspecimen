package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class VisitUidLabelToken extends AbstractUniqueIdToken<Specimen> {
	@Autowired
	private DaoFactory daoFactory;

	public VisitUidLabelToken() {
		this.name = "VISIT_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		return daoFactory.getUniqueIdGenerator().getUniqueId(name, specimen.getVisit().getId().toString());
	}
}
