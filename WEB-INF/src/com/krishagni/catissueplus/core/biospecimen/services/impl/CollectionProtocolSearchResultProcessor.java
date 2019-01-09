package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class CollectionProtocolSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return CollectionProtocol.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyMap();
		}

		CpListCriteria crit = new CpListCriteria().ids(entityIds).siteCps(siteCps).includePi(false);
		List<CollectionProtocolSummary> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocols(crit);
		return cps.stream().collect(Collectors.toMap(CollectionProtocolSummary::getId, this::getProps));
	}

	private Map<String, Object> getProps(CollectionProtocolSummary cp) {
		Map<String, Object> props = new HashMap<>();
		props.put("shortTitle", cp.getShortTitle());
		props.put("title", cp.getTitle());
		if (StringUtils.isNotBlank(cp.getCode())) {
			props.put("code", cp.getCode());
		}

		return props;
	}
}