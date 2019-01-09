package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class SpecimenListSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return SpecimenList.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		SpecimenListsCriteria crit = new SpecimenListsCriteria().ids(entityIds);
		if (!AuthUtil.isAdmin()) {
			crit.userId(AuthUtil.getCurrentUser().getId());
		}

		List<SpecimenListSummary> carts = daoFactory.getSpecimenListDao().getSpecimenLists(crit);
		return carts.stream().collect(Collectors.toMap(SpecimenListSummary::getId, this::getProps));
	}

	private Map<String, Object> getProps(SpecimenListSummary cart) {
		return Collections.singletonMap("name", cart.getName());
	}
}
