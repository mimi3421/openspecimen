package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.PvValidator;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {

	@Override
	public LabelPrintRule fromRuleDef(Map<String, String> ruleDef, boolean failOnError, OpenSpecimenException ose) {
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();

		setCps(ruleDef, failOnError, rule, ose);
		setVisitSite(ruleDef, failOnError, rule, ose);
		setLineage(ruleDef, failOnError, rule, ose);
		setSpecimenClasses(ruleDef, failOnError, rule, ose);
		setSpecimenTypes(ruleDef, failOnError, rule, ose);
		return rule;
	}

	private void setCps(Map<String, String> input, boolean failOnError, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> inputList = Utility.csvToStringList(input.get("cps"));
		if (inputList.isEmpty()) {
			inputList = Utility.csvToStringList(input.get("cpShortTitle")); // backward compatibility
		}

		if (inputList.isEmpty()) {
			return;
		}

		List<CollectionProtocol> result = new ArrayList<>();
		Pair<List<Long>, List<String>> idsAndTitles = getIdsAndNames(inputList);

		if (!idsAndTitles.first().isEmpty()) {
			List<CollectionProtocol> cps = getList(
				(ids) -> daoFactory.getCollectionProtocolDao().getByIds(ids),
				idsAndTitles.first(), (cp) -> cp.getId(),
				failOnError ? ose : null, failOnError ? CpErrorCode.DOES_NOT_EXIST : null);
			result.addAll(cps);
		}

		if (!idsAndTitles.second().isEmpty()) {
			List<CollectionProtocol> cps = getList(
				(titles) -> daoFactory.getCollectionProtocolDao().getCpsByShortTitle(titles),
				idsAndTitles.second(), (cp) -> cp.getShortTitle(),
				failOnError ? ose : null, failOnError ? CpErrorCode.DOES_NOT_EXIST : null);
			result.addAll(cps);
		}

		rule.setCps(result);
	}

	private void setVisitSite(Map<String, String> input, boolean failOnError, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String visitSite = input.get("visitSite");
		if (StringUtils.isBlank(visitSite)) {
			return;
		}

		Site site = null;
		try {
			site = daoFactory.getSiteDao().getById(Long.parseLong(visitSite));
		} catch (NumberFormatException nfe) {
			site = daoFactory.getSiteDao().getSiteByName(visitSite);
		}

		if (failOnError && site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, visitSite);
		}

		rule.setVisitSite(site);
	}

	private void setLineage(Map<String, String> input, boolean failOnError, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String lineage = input.get("lineage");
		if (StringUtils.isBlank(lineage)) {
			return;
		}

		if (!Specimen.isValidLineage(lineage)) {
			ose.addError(SpecimenErrorCode.INVALID_LINEAGE, lineage);
		}

		rule.setLineage(lineage);
	}

	private void setSpecimenClasses(Map<String, String> inputMap, boolean failOnError, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> specimenClasses = Utility.csvToStringList(inputMap.get("specimenClasses"));
		rule.setSpecimenClasses(specimenClasses);
		if (specimenClasses.isEmpty() || !failOnError) {
			return;
		}

		if (!PvValidator.areValid("specimen_type", specimenClasses)) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, specimenClasses);
		}
	}

	private void setSpecimenTypes(Map<String, String> input, boolean failOnError, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> types = Utility.csvToStringList(input.get("specimenTypes"));
		rule.setSpecimenTypes(types);
		if (types.isEmpty() || !failOnError) {
			String type = input.get("specimenType");
			if (StringUtils.isBlank(type)) {
				return;
			}

			types = Collections.singletonList(type);
			rule.setSpecimenTypes(types);
		}

		if (!PvValidator.areValid("specimen_type", types)) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, types);
		}
	}
}
