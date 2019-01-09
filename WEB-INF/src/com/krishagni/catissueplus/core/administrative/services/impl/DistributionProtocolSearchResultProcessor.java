package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class DistributionProtocolSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return DistributionProtocol.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessDistributionProtocolSites();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyMap();
		}

		DpListCriteria crit = new DpListCriteria().ids(entityIds).sites(siteCps);
		List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(crit);
		return dps.stream().collect(Collectors.toMap(DistributionProtocol::getId, this::getProps));
	}

	private Map<String, Object> getProps(DistributionProtocol dp) {
		Map<String, Object> props = new HashMap<>();
		props.put("shortTitle", dp.getShortTitle());
		props.put("title", dp.getTitle());
		if (StringUtils.isNotBlank(dp.getIrbId())) {
			props.put("irbId", dp.getIrbId());
		}

		return props;
	}
}
