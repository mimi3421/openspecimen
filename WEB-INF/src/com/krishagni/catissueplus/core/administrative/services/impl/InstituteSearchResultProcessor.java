package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.repository.InstituteListCriteria;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class InstituteSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Institute.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		InstituteListCriteria crit = new InstituteListCriteria().ids(entityIds);
		List<InstituteDetail> institutes = daoFactory.getInstituteDao().getInstitutes(crit);
		return institutes.stream().collect(Collectors.toMap(InstituteDetail::getId, this::getProps));
	}

	private Map<String, Object> getProps(InstituteDetail detail) {
		return Collections.singletonMap("name", detail.getName());
	}
}
