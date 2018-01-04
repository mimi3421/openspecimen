package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface StagedVisitDao extends Dao<StagedVisit> {
	StagedVisit getByAccessionNo(String accessionNo);

	List<StagedVisit> getByEmpiOrMrn(String empiOrMrn);
}
