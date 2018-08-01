package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public class SpecimenLabelPrintRule extends LabelPrintRule {
	private List<String> cps = new ArrayList<>();

	private String visitSite;

	private String specimenClass;
	
	private String specimenType;

	private String lineage;

	public void setCpShortTitle(String cpShortTitle) {
		cps = new ArrayList<>();
		cps.add(cpShortTitle);
	}

	public List<String> getCps() {
		return cps;
	}

	public void setCps(List<String> cps) {
		this.cps = cps;
	}

	public String getVisitSite() {
		return visitSite;
	}

	public void setVisitSite(String visitSite) {
		this.visitSite = visitSite;
	}

	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
	}

	public String getSpecimenType() {
		return specimenType;
	}

	public void setSpecimenType(String specimenType) {
		this.specimenType = specimenType;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		if (!isValidLineage(lineage)) {
			throw new IllegalArgumentException("Invalid lineage: " + lineage + " Expected: New, Derived or Aliquot");
		}

		this.lineage = lineage;
	}

	public boolean isApplicableFor(Specimen specimen, User user, String ipAddr) {
		if (!super.isApplicableFor(user, ipAddr)) {
			return false;
		}

		if (CollectionUtils.isNotEmpty(cps) && !cps.contains(specimen.getCollectionProtocol().getShortTitle())) {
			return false;
		}

		Visit visit = specimen.getVisit();
		if (!isWildCard(visitSite) && (visit.getSite() == null || !visit.getSite().getName().equals(visitSite))) {
			return false;
		}

		if (!isWildCard(specimenClass) && !specimen.getSpecimenClass().equals(specimenClass)) {
			return false;
		}
		
		if (!isWildCard(specimenType) && !specimen.getSpecimenType().equals(specimenType)) {
			return false;
		}

		if (!isWildCard(lineage) && !specimen.getLineage().equals(lineage)) {
			return false;
		}
		
		return true;
	}

	@Override
	protected Map<String, String> getDefMap() {
		Map<String, String> ruleDef = new HashMap<>();
		ruleDef.put("cps", String.join(",", getCps()));
		ruleDef.put("visitSite", getVisitSite());
		ruleDef.put("specimenClass", getSpecimenClass());
		ruleDef.put("specimenType", getSpecimenType());
		ruleDef.put("lineage", getLineage());
		return ruleDef;
	}

	public String toString() {
		return new StringBuilder(super.toString())
			.append(", cp = ").append(String.join(",", getCps()))
			.append(", lineage = ").append(getLineage())
			.append(", visit site = ").append(getVisitSite())
			.append(", specimen class = ").append(getSpecimenClass())
			.append(", specimen type = ").append(getSpecimenType())
			.toString();
	}

	private boolean isValidLineage(String lineage) {
		return isWildCard(lineage) || Specimen.isValidLineage(lineage);
	}
}
