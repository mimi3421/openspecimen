package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.biospecimen.repository.PdeNotifDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class PdeNotifDaoImpl extends AbstractDao<PdeNotif> implements PdeNotifDao {

	@Override
	public Class<PdeNotif> getType() {
		return PdeNotif.class;
	}

	@Override
	public void updateLinkStatus(String type, Long tokenId, String status) {
		getCurrentSession().getNamedQuery(UPDATE_LINK_STATUS)
			.setParameter("status", status)
			.setParameter("formType", type)
			.setParameter("tokenId", tokenId)
			.executeUpdate();
	}

	private static final String FQN = PdeNotif.class.getName();

	private static final String UPDATE_LINK_STATUS = FQN + ".updateLinkStatus";
}
