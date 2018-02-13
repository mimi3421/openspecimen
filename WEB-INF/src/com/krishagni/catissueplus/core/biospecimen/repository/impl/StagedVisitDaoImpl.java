package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedVisitDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StagedVisitDaoImpl extends AbstractDao<StagedVisit> implements StagedVisitDao {
	@Override
	public Class<StagedVisit> getType() {
		return StagedVisit.class;
	}

	@Override
	public StagedVisit getByName(String name) {
		return (StagedVisit) getCurrentSession().getNamedQuery(GET_BY_NAME)
			.setParameter("name", name.toLowerCase())
			.uniqueResult();
	}

	@Override
	public StagedVisit getBySprNo(String sprNo) {
		return (StagedVisit) getCurrentSession().getNamedQuery(GET_BY_SPR_NO)
			.setParameter("sprNo", sprNo)
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

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_BY_SPR_NO = FQN + ".getBySprNo";

	private static final String GET_BY_EMPI_OR_MRN = FQN + ".getByEmpiOrMrn";
}
