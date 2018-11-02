package com.krishagni.catissueplus.core.administrative.domain;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class ContainerLabelPrintRule extends LabelPrintRule {
	private Long siteId;

	public Long getSiteId() {
		return siteId;
	}

	public void setSiteId(Long siteId) {
		this.siteId = siteId;
	}

	public boolean isApplicableFor(StorageContainer container, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		return getSiteId() == null || getSiteId().equals(container.getSite().getId());
	}

	@Override
	protected Map<String, String> getDefMap() {
		Map<String, String> ruleDef = new HashMap<>();
		ruleDef.put("site", getSiteId() != null ? getSiteId().toString() : null);
		return ruleDef;
	}

	public String toString() {
		return super.toString() + ", site = " + getSiteId();
	}
}
