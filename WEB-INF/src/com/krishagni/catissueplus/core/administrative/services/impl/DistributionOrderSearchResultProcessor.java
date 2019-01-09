package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class DistributionOrderSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return DistributionOrder.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessDistributionOrderSites();
		if (sites != null && sites.isEmpty()) {
			return Collections.emptyMap();
		}

		DistributionOrderListCriteria crit = new DistributionOrderListCriteria().ids(entityIds).sites(sites);
		List<DistributionOrderSummary> orders = daoFactory.getDistributionOrderDao().getOrders(crit);
		return orders.stream().collect(Collectors.toMap(DistributionOrderSummary::getId, this::getProps));
	}

	private Map<String, Object> getProps(DistributionOrderSummary order) {
		return Collections.singletonMap("name", order.getName());
	}
}