package com.krishagni.catissueplus.core.administrative.services;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;

public interface AutomatedFreezer {
	ContainerStoreList.Status processList(ContainerStoreList list);
}
