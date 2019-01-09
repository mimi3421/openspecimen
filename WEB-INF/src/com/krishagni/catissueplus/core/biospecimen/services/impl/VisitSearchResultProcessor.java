package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class VisitSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Visit.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyMap();
		}

		VisitsListCriteria crit = new VisitsListCriteria()
			.ids(entityIds)
			.siteCps(siteCps)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

		List<Visit> visits = daoFactory.getVisitsDao().getVisitsList(crit);
		return visits.stream().collect(Collectors.toMap(Visit::getId, this::getProps));
	}

	private Map<String, Object> getProps(Visit visit) {
		Map<String, Object> props = new HashMap<>();
		props.put("cp", visit.getCollectionProtocol().getShortTitle());
		props.put("name", visit.getName());
		return props;
	}
}