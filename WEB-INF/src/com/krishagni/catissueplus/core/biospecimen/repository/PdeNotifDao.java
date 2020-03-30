package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface PdeNotifDao extends Dao<PdeNotif> {
	void updateLinkStatus(String type, Long tokenId, String status);
}
