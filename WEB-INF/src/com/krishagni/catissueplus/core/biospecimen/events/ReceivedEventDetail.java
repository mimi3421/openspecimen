package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionReceiveDetail;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;

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

		detail.setReceivedQuality(PermissibleValue.getValue(sre.getQuality()));
		return detail;
	}

	public static ReceivedEventDetail from(SpecimenCollectionReceiveDetail cre) {
		if (cre == null) {
			return null;
		}

		ReceivedEventDetail re = new ReceivedEventDetail();
		re.setReceivedQuality(cre.getRecvQuality());
		re.setTime(cre.getRecvTime());
		re.setUser(UserSummary.from(cre.getReceiver()));
		return re;
	}
}