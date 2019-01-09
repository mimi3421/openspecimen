package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchResultProcessor;

public class ContainerSearchResultProcessor extends AbstractSearchResultProcessor {
	@Override
	public String getEntity() {
		return StorageContainer.getEntityName();
	}

	@Override
	protected Map<Long, Map<String, Object>> getEntityProps(List<Long> entityIds) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyMap();
		}

		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.ids(entityIds)
			.siteCps(siteCps);

		List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
		return containers.stream().collect(Collectors.toMap(StorageContainer::getId, this::getProps));
	}

	private Map<String, Object> getProps(StorageContainer container) {
		Map<String, Object> props = new HashMap<>();
		props.put("name", container.getName());
		props.put("usedFor", container.getUsedFor());
		props.put("site", container.getSite().getName());
		return props;
	}
}
