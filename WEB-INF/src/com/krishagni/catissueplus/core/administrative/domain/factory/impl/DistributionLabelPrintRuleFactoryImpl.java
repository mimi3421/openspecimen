package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.DistributionLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;

public class DistributionLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {
	@Override
	public LabelPrintRule fromRuleDef(Map<String, String> ruleDef, OpenSpecimenException ose) {
		DistributionLabelPrintRule rule = new DistributionLabelPrintRule();

		setDps(ruleDef, rule, ose);
		return rule;
	}

	private void setDps(Map<String, String> ruleDef, DistributionLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> dpShortTitles = Utility.csvToStringList(ruleDef.get("dps"));
		if (dpShortTitles.isEmpty()) {
			return;
		}

		List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(dpShortTitles);
		if (dps.size() != dpShortTitles.size()) {
			Set<String> dbTitles = dps.stream().map(DistributionProtocol::getShortTitle).collect(Collectors.toSet());
			ose.addError(
				DistributionProtocolErrorCode.INV_DPS,
				dpShortTitles.stream().filter(i -> !dbTitles.contains(i)).collect(Collectors.toSet()));
			return;
		}

		rule.setDps(dpShortTitles);
	}
}
