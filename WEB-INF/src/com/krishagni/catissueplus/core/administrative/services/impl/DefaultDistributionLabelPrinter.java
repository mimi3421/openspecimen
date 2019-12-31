package com.krishagni.catissueplus.core.administrative.services.impl;

import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.administrative.domain.DistributionLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.services.impl.AbstractLabelPrinter;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class DefaultDistributionLabelPrinter extends AbstractLabelPrinter<DistributionOrderItem> implements ApplicationListener<OpenSpecimenEvent> {
	@Override
	protected boolean isApplicableFor(LabelPrintRule rule, DistributionOrderItem orderItem, User user, String ipAddr) {
		DistributionLabelPrintRule distLabelRule = (DistributionLabelPrintRule) rule;
		return distLabelRule.isApplicableFor(orderItem, user, ipAddr);
	}

	@Override
	protected String getObjectType() {
		return "ORDER_ITEM";
	}

	@Override
	protected String getItemType() {
		return DistributionOrderItem.getEntityName();
	}

	@Override
	protected String getItemLabel(DistributionOrderItem orderItem) {
		return orderItem.getId().toString();
	}

	@Override
	protected Long getItemId(DistributionOrderItem orderItem) {
		return orderItem.getId();
	}
}
