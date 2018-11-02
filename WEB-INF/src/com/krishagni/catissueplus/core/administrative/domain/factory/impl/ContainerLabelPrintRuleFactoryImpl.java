package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class ContainerLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {
	@Override
	public LabelPrintRule fromRuleDef(Map<String, String> ruleDef, OpenSpecimenException ose) {
		ContainerLabelPrintRule rule = new ContainerLabelPrintRule();

		setSiteId(ruleDef, rule, ose);
		return rule;
	}

	private void setSiteId(Map<String, String> ruleDef, ContainerLabelPrintRule rule, OpenSpecimenException ose) {
		String input = ruleDef.get("site");
		if (StringUtils.isBlank(input)) {
			return;
		}

		Site site = null;
		try {
			site = daoFactory.getSiteDao().getById(Long.parseLong(input));
		} catch (NumberFormatException nfe) {
			site = daoFactory.getSiteDao().getSiteByName(input);
		}

		if (site == null) {
			throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND, input);
		}

		rule.setSiteId(site.getId());
	}
}
