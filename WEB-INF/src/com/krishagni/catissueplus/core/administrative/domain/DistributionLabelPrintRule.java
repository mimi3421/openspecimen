package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class DistributionLabelPrintRule extends LabelPrintRule {
	private List<String> dps = new ArrayList<>();

	public List<String> getDps() {
		return dps;
	}

	public void setDps(List<String> dps) {
		this.dps = dps;
	}

	public boolean isApplicableFor(DistributionOrderItem item, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		if (CollectionUtils.isNotEmpty(dps) && !dps.contains(dpShortTitle(item))) {
			return false;
		}

		return true;
	}

	@Override
	protected Map<String, String> getDefMap() {
		Map<String, String> ruleDef = new HashMap<>();
		ruleDef.put("dps", String.join(",", getDps()));
		return ruleDef;
	}

	public String toString() {
		return super.toString() + ", dp = " + String.join(",", getDps());
	}

	private String dpShortTitle(DistributionOrderItem item) {
		return item.getOrder().getDistributionProtocol().getShortTitle();
	}
}
