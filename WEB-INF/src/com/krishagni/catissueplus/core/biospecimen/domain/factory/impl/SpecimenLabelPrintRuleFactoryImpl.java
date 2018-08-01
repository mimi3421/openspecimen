package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.PvValidator;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {

	@Override
	public LabelPrintRule fromRuleDef(Map<String, String> ruleDef, OpenSpecimenException ose) {
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();

		setCps(ruleDef, rule, ose);
		setVisitSite(ruleDef, rule, ose);
		setLineage(ruleDef, rule, ose);
		setSpecimenClass(ruleDef, rule, ose);
		setSpecimenType(ruleDef, rule, ose);
		return rule;
	}

	private void setCps(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> cpShortTitles = Utility.csvToStringList(input.get("cps"));
		if (cpShortTitles.isEmpty()) {
			cpShortTitles = Utility.csvToStringList(input.get("cpShortTitle")); // backward compatibility
		}

		if (CollectionUtils.isEmpty(cpShortTitles)) {
			return;
		}

		List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCpsByShortTitle(cpShortTitles);
		if (cps.size() != cpShortTitles.size()) {
			Set<String> dbTitles = cps.stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet());
			ose.addError(CpErrorCode.DOES_NOT_EXIST, cpShortTitles.stream().filter(i -> !dbTitles.contains(i)).collect(Collectors.toSet()));
			return;
		}

		rule.setCps(cpShortTitles);
	}

	private void setVisitSite(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String visitSite = input.get("visitSite");
		if (StringUtils.isBlank(visitSite)) {
			return;
		}

		Site site = daoFactory.getSiteDao().getSiteByName(visitSite);
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, visitSite);
			return;
		}

		rule.setVisitSite(site.getName());
	}

	private void setLineage(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String lineage = input.get("lineage");
		if (StringUtils.isBlank(lineage)) {
			return;
		}

		if (!Specimen.isValidLineage(lineage)) {
			ose.addError(SpecimenErrorCode.INVALID_LINEAGE, lineage);
		}

		rule.setLineage(lineage);
	}

	private void setSpecimenClass(Map<String, String> inputMap, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String specimenClass = inputMap.get("specimenClass");
		if (StringUtils.isBlank(specimenClass)) {
			return;
		}

		if (!PvValidator.isValid("specimen_type", specimenClass)) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, specimenClass);
			return;
		}

		rule.setSpecimenClass(specimenClass);
	}

	private void setSpecimenType(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String specimenType = input.get("specimenType");
		if (StringUtils.isBlank(specimenType)) {
			return;
		}

		boolean isValid;
		if (StringUtils.isNotBlank(rule.getSpecimenClass())) {
			isValid = PvValidator.isValid("specimen_type", rule.getSpecimenClass(), specimenType);
		} else {
			isValid = PvValidator.isValid("specimen_type", specimenType, true);
		}

		if (!isValid) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, specimenType);
		}

		rule.setSpecimenType(specimenType);
	}
}
