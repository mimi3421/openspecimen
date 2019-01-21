package com.krishagni.catissueplus.core.administrative.services;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.util.CsvWriter;

public interface ContainerDefragmenter {
	int defragment(StorageContainer container);
}
