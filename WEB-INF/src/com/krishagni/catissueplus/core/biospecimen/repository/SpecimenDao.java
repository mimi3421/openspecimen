
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenDao extends Dao<Specimen> {
	List<Specimen> getSpecimens(SpecimenListCriteria crit);

	List<Long> getSpecimenIds(SpecimenListCriteria crit);
	
	Specimen getByLabel(String label);

	Specimen getByLabelAndCp(String cpShortTitle, String label);

	Specimen getByBarcode(String barcode);

	Specimen getByBarcodeAndCp(String cpShortTitle, String barcode);
	
	List<Specimen> getSpecimensByIds(List<Long> specimenIds);
	
	List<Specimen> getSpecimensByVisitId(Long visitId);
	
	List<Specimen> getSpecimensByVisitName(String visitName);
	
	Specimen getSpecimenByVisitAndSr(Long visitId, Long srId);

	List<Specimen> getByVisitAndSrCode(Long visitId, Collection<String> reqCodes);

	Specimen getParentSpecimenByVisitAndSr(Long visitId, Long srId);

	Map<String, Object> getCprAndVisitIds(String key, Object value);
	
	Map<Long, Set<Long>> getSpecimenSites(Set<Long> specimenIds);

	Map<Long, String> getDistributionStatus(List<Long> specimenIds);

	String getDistributionStatus(Long specimenId);

	List<Visit> getSpecimenVisits(SpecimenListCriteria crit);

	boolean areDuplicateLabelsPresent();

	boolean areDuplicateBarcodesPresent();

	Map<Long, Long> getSpecimenStorageSite(Set<Long> specimenIds);

	List<String> getNonCompliantSpecimens(SpecimenListCriteria crit);
}
