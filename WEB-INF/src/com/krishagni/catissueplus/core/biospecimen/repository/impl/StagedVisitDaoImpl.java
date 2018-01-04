package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedVisitDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StagedVisitDaoImpl extends AbstractDao<StagedVisit> implements StagedVisitDao {
	@Override
	public StagedVisit getByAccessionNo(String accessionNo) {
		return (StagedVisit) getCurrentSession().getNamedQuery(GET_BY_ACC_NO)
			.setParameter("accessionNo", accessionNo.toLowerCase())
			.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StagedVisit> getByEmpiOrMrn(String empiOrMrn) {
		return getCurrentSession().getNamedQuery(GET_BY_EMPI_OR_MRN)
			.setParameter("empiOrMrn", empiOrMrn.toLowerCase())
			.list();
	}

	private static final String FQN = StagedVisit.class.getName();

	private static final String GET_BY_ACC_NO = FQN + ".getByAccessionNo";

	private static final String GET_BY_EMPI_OR_MRN = FQN + ".getByEmpiOrMrn";
}
