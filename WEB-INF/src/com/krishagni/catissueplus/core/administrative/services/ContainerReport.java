package com.krishagni.catissueplus.core.administrative.services;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;

public interface ContainerReport {
	String getName();

	ExportedFileDetail generate(StorageContainer container, Object... params);
}
