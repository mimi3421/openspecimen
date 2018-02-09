package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;

public class ReceivedEventDetail extends SpecimenEventDetail {
	private String receivedQuality;

	public String getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(String receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public static ReceivedEventDetail from(SpecimenReceivedEvent sre) {
		if (sre == null) {
			return null;
		}

		ReceivedEventDetail detail = new ReceivedEventDetail();
		fromTo(sre, detail);

		detail.setReceivedQuality(sre.getQuality());
		return detail;
	}
}