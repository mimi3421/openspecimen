package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionEvent;

public class CollectionEventDetail extends SpecimenEventDetail {
	private String procedure;
	
	private String container;

	public String getProcedure() {
		return procedure;
	}

	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public static CollectionEventDetail from(SpecimenCollectionEvent ce) {
		if (ce == null) {
			return null;
		}

		CollectionEventDetail detail = new CollectionEventDetail();
		fromTo(ce, detail);

		detail.setContainer(ce.getContainer());
		detail.setProcedure(ce.getProcedure());
		return detail;
	}
}
