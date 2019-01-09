package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class SiteSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return Site.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		SiteListCriteria crit = new SiteListCriteria().ids(entityIds);
		List<Site> sites = AccessCtrlMgr.getInstance().getAccessibleSites(crit);
		return sites.stream().collect(Collectors.toMap(Site::getId, this::getProps));
	}

	private Map<String, Object> getProps(Site site) {
		Map<String, Object> props = new HashMap<>();
		props.put("name", site.getName());
		if (StringUtils.isBlank(site.getCode())) {
			props.put("code", site.getCode());
		}

		return props;
	}
}