package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRule extends LabelPrintRule {
	private List<CollectionProtocol> cps = new ArrayList<>();

	private Site visitSite;

	private String specimenClass;
	
	private String specimenType;

	private String lineage;

	public void setCp(CollectionProtocol cp) {
		cps = new ArrayList<>();
		cps.add(cp);
	}

	public List<CollectionProtocol> getCps() {
		return cps;
	}

	public void setCps(List<CollectionProtocol> cps) {
		this.cps = cps;
	}

	public Site getVisitSite() {
		return visitSite;
	}

	public void setVisitSite(Site visitSite) {
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

		if (CollectionUtils.isNotEmpty(cps) && !cps.stream().anyMatch(cp -> cp.equals(specimen.getCollectionProtocol()))) {
			return false;
		}

		Visit visit = specimen.getVisit();
		if (visitSite != null && !visitSite.equals(visit.getSite())) {
			return false;
		}

		if (StringUtils.isNotBlank(specimenClass) && !specimenClass.equals(specimen.getSpecimenClass())) {
			return false;
		}
		
		if (StringUtils.isNotBlank(specimenType) && !specimenType.equals(specimen.getSpecimenType())) {
			return false;
		}

		if (StringUtils.isNotBlank(lineage) && !lineage.equals(specimen.getLineage())) {
			return false;
		}
		
		return true;
	}

	@Override
	protected Map<String, String> getDefMap(boolean ufn) {
		Map<String, String> ruleDef = new HashMap<>();

		ruleDef.put("cps", getCpList(ufn));
		ruleDef.put("visitSite", getSite(ufn, getVisitSite()));
		ruleDef.put("specimenClass", getSpecimenClass());
		ruleDef.put("specimenType", getSpecimenType());
		ruleDef.put("lineage", getLineage());
		return ruleDef;
	}

	public String toString() {
		return new StringBuilder(super.toString())
			.append(", cp = ").append(getCpList(true))
			.append(", lineage = ").append(getLineage())
			.append(", visit site = ").append(getSite(true, getVisitSite()))
			.append(", specimen class = ").append(getSpecimenClass())
			.append(", specimen type = ").append(getSpecimenType())
			.toString();
	}

	private boolean isValidLineage(String lineage) {
		return isWildCard(lineage) || Specimen.isValidLineage(lineage);
	}

	private String getCpList(boolean ufn) {
		Function<CollectionProtocol, String> cpMapper = ufn ? (cp) -> cp.getShortTitle() : (cp) -> cp.getId().toString();
		return Utility.nullSafeStream(getCps()).map(cpMapper).collect(Collectors.joining(","));
	}

	private String getSite(boolean ufn, Site site) {
		return site != null ? (ufn ? site.getName() : site.getId().toString()) : null;
	}
}
