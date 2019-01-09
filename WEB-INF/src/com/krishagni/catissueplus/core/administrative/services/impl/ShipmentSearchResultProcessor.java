package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class ShipmentSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Shipment.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessShipmentSites();
		if (sites != null && sites.isEmpty()) {
			return Collections.emptyMap();
		}

		ShipmentListCriteria crit = new ShipmentListCriteria().sites(sites).ids(entityIds);
		List<Shipment> shipments = daoFactory.getShipmentDao().getShipments(crit);
		return shipments.stream().collect(Collectors.toMap(Shipment::getId, this::getProps));
	}

	private Map<String, Object> getProps(Shipment shipment) {
		return Collections.singletonMap("name", shipment.getName());
	}
}
