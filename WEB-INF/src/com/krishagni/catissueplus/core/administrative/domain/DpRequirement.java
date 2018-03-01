package com.krishagni.catissueplus.core.administrative.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.util.Status;

@Audited
public class DpRequirement extends BaseExtensionEntity {
	public static final String EXTN = "DpRequirementExtension";

	private DistributionProtocol distributionProtocol;
	
	private String specimenType;
	
	private String anatomicSite;
	
	private Set<String> pathologyStatuses = new HashSet<>();

	private String clinicalDiagnosis;

	private BigDecimal cost;
	
	private Long specimenCount;
	
	private BigDecimal quantity;
	
	private String comments;
	
	private String activityStatus;

	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}

	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
	}
	
	public String getSpecimenType() {
		return specimenType;
	}
	
	public void setSpecimenType(String specimenType) {
		this.specimenType = specimenType;
	}
	
	public String getAnatomicSite() {
		return anatomicSite;
	}
	
	public void setAnatomicSite(String anatomicSite) {
		this.anatomicSite = anatomicSite;
	}
	
	public Set<String> getPathologyStatuses() {
		return pathologyStatuses;
	}
	
	public void setPathologyStatuses(Set<String> pathologyStatuses) {
		this.pathologyStatuses = pathologyStatuses;
	}

	public String getClinicalDiagnosis() {
		return clinicalDiagnosis;
	}

	public void setClinicalDiagnosis(String clinicalDiagnosis) {
		this.clinicalDiagnosis = clinicalDiagnosis;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public Long getSpecimenCount() {
		return specimenCount;
	}
	
	public void setSpecimenCount(Long specimenCount) {
		this.specimenCount = specimenCount;
	}
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public String getComments() {
		return comments;
	}
	
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public String getActivityStatus() {
		return activityStatus;
	}
	
	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	public void update(DpRequirement dpr) {
		setDistributionProtocol(dpr.getDistributionProtocol());
		setSpecimenType(dpr.getSpecimenType());
		setAnatomicSite(dpr.getAnatomicSite());
		CollectionUpdater.update(getPathologyStatuses(), dpr.getPathologyStatuses());
		setClinicalDiagnosis(dpr.getClinicalDiagnosis());
		setCost(dpr.getCost());
		setSpecimenCount(dpr.getSpecimenCount());
		setQuantity(dpr.getQuantity());
		setComments(dpr.getComments());
		setActivityStatus(dpr.getActivityStatus());
		setExtension(dpr.getExtension());
	}
	
	public boolean equalsSpecimenGroup(DpRequirement dpr) {
		return equalsSpecimenGroup(dpr.getSpecimenType(), dpr.getAnatomicSite(), dpr.getPathologyStatuses(), dpr.getClinicalDiagnosis());
	}

	public boolean equalsSpecimenGroup(String specimenType, String anatomicSite, Set<String> pathologyStatuses, String clinicalDiagnosis) {
		return StringUtils.equals(getSpecimenType(), specimenType) &&
				StringUtils.equals(getAnatomicSite(), anatomicSite) &&
				arePathologyStatusesEqual(pathologyStatuses) &&
				StringUtils.equals(getClinicalDiagnosis(), clinicalDiagnosis);
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public int getMatchPoints(Specimen specimen) {
		int points = 0;
		if (StringUtils.isNotBlank(getSpecimenType())) {
			if (!getSpecimenType().equals(specimen.getSpecimenType())) {
				return 0;
			}

			points += 40;
		}

		if (!getPathologyStatuses().isEmpty()) {
			if (!getPathologyStatuses().contains(specimen.getPathologicalStatus())) {
				return 0;
			}

			points += 30;
		}

		if (StringUtils.isNotBlank(getAnatomicSite())) {
			if (!getAnatomicSite().equals(specimen.getTissueSite())) {
				return 0;
			}

			points += 20;
		}

		if (StringUtils.isNotBlank(getClinicalDiagnosis())) {
			if (!specimen.getVisit().getClinicalDiagnoses().contains(getClinicalDiagnosis())) {
				return 0;
			}

			points += 10;
		}

		return points;
	}

	private boolean arePathologyStatusesEqual(Set<String> pathologyStatuses) {
		boolean isEmptyOldPaths = CollectionUtils.isEmpty(getPathologyStatuses());
		boolean isEmptyNewPaths = CollectionUtils.isEmpty(pathologyStatuses);

		if (isEmptyOldPaths && isEmptyNewPaths) {
			return true;
		}

		if (isEmptyOldPaths || isEmptyNewPaths) {
			return false;
		}

		return CollectionUtils.isSubCollection(pathologyStatuses, getPathologyStatuses()) ||
				CollectionUtils.isSubCollection(getPathologyStatuses(), pathologyStatuses);
	}
}
