package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface PdeNotifDao extends Dao<PdeNotif> {
	void updateLinkStatus(String type, Long tokenId, String status);

	List<PdeNotif> getPendingNotifs(long lastId, int maxResults);

	List<PdeNotif> getNotifs(PdeNotifListCriteria crit);
}
