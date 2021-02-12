package com.krishagni.catissueplus.core.administrative.repository;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface StorageContainerPositionDao extends Dao<StorageContainerPosition> {
	Long getSpecimenIdByPosition(Long containerId, String row, String column);
}
