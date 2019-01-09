package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class SpecimenSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Specimen.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyMap();
		}

		SpecimenListCriteria crit = new SpecimenListCriteria()
			.ids(entityIds)
			.siteCps(siteCps)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

		List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit);
		return specimens.stream().collect(Collectors.toMap(Specimen::getId, this::getProps));
	}

	private Map<String, Object> getProps(Specimen specimen) {
		Map<String, Object> props = new HashMap<>();
		props.put("cp", specimen.getCollectionProtocol().getShortTitle());
		props.put("label", specimen.getLabel());
		props.put("specimenClass", specimen.getSpecimenClass());
		props.put("type", specimen.getSpecimenType());

		if (StringUtils.isNotBlank(specimen.getBarcode())) {
			props.put("barcode", specimen.getBarcode());
		}
		return props;
	}
}