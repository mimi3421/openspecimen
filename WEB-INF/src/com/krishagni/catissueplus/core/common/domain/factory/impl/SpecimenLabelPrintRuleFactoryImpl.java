package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule.CmdFileFmt;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.service.PvValidator;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenLabelPrintRuleFactoryImpl implements LabelPrintRuleFactory {
	private DaoFactory daoFactory;

	private LabelTmplTokenRegistrar printLabelTokensRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	@Override
	public LabelPrintRule createLabelPrintRule(Map<String, String> def) {
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		setLabelType(def, rule, ose);
		setPrinterName(def, rule, ose);
		setCmdFilesDir(def, rule, ose);
		setCps(def, rule, ose);
		setSpecimenClass(def, rule, ose);
		setSpecimenType(def, rule, ose);
		setDataTokens(def, rule, ose);
		setIpAddressMatcher(def, rule, ose);
		setLabelDesign(def, rule, ose);
		setCmdFileFmt(def, rule, ose);
		setLineage(def, rule, ose);
		setVisitSite(def, rule, ose);
		setUsers(def, rule, ose);
		ose.checkAndThrow();
		return rule;
	}

	private void setLabelType(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelType"))) {
			rule.setLabelType("Std");
			return;
		}

		rule.setLabelType(input.get("labelType"));
	}

	private void setPrinterName(Map<String, String> input,SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("printerName"))) {
			rule.setPrinterName("default");
			return;
		}

		rule.setPrinterName(input.get("printerName"));
	}

	private void setCmdFilesDir(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String dirPath = input.get("cmdFilesDir");
		if (StringUtils.isBlank(dirPath)) {
			ose.addError(PrintRuleConfigErrorCode.CMD_FILES_DIR_REQ);
			return;
		}

		if (dirPath.equals("*")) {
			dirPath = getDefaultPrintLabelsDir();
		}

		File dir = new File(dirPath);
		boolean dirCreated = true;
		if (!dir.exists()) {
			dirCreated = dir.mkdirs();
		}

		if (!dirCreated || !dir.isDirectory()) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILES_DIR, dir.getAbsolutePath());
			return;
		}

		rule.setCmdFilesDir(dirPath);
	}

	private String getDefaultPrintLabelsDir() {
		return ConfigUtil.getInstance().getDataDir() + File.separator + "print-labels";
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
			ose.addError(PrintRuleConfigErrorCode.INVALID_CPS);
			return;
		}

		rule.setCps(cpShortTitles);
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

	private void setDataTokens(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("dataTokens"))) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKENS_REQ);
			return;
		}

		List<String> invalidTokenNames = new ArrayList<>();
		List<LabelTmplToken> dataTokens = new ArrayList<>();

		List<String> tokenNames = Utility.csvToStringList(input.get("dataTokens"));
		for (String key : tokenNames) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(key);
			if (token == null) {
				invalidTokenNames.add(key);
			} else {
				dataTokens.add(token);
			}
		}

		if (CollectionUtils.isNotEmpty(invalidTokenNames)) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKEN_NOT_FOUND, invalidTokenNames, invalidTokenNames.size());
		}

		rule.setDataTokens(dataTokens);
	}

	private void setIpAddressMatcher(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String ipRange = input.get("ipAddressMatcher");
		if (StringUtils.isBlank(ipRange)) {
			return;
		}

		IpAddressMatcher ipAddressMatcher = null;
		try {
			ipAddressMatcher = new IpAddressMatcher(ipRange);
		} catch (IllegalArgumentException e) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_IP_RANGE, ipRange);
			return;
		}

		rule.setIpAddressMatcher(ipAddressMatcher);
	}

	private void setLabelDesign(Map<String, String> input,SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelDesign"))) {
			return;
		}

		rule.setLabelDesign(input.get("labelDesign"));
	}

	private void setCmdFileFmt(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		String cmdFileFmt = input.get("cmdFileFmt");
		if (StringUtils.isBlank(cmdFileFmt)) {
			return;
		}

		if (CmdFileFmt.get(cmdFileFmt) == null) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILE_FMT, cmdFileFmt);
		}

		rule.setCmdFileFmt(cmdFileFmt);
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

	private void setUsers(Map<String, String> input, SpecimenLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> userLogins = Utility.csvToStringList(input.get("users"));
		if (userLogins.isEmpty()) {
			userLogins = Utility.csvToStringList(input.get("userLogin")); // backward compatibility
		}

		if (CollectionUtils.isEmpty(userLogins)) {
			return;

		}

		String domainName = input.get("domainName");
		if (StringUtils.isNotBlank(domainName)) {
			AuthDomain domain = daoFactory.getAuthDao().getAuthDomainByName(domainName);
			if (domain == null) {
				ose.addError(UserErrorCode.DOMAIN_NOT_FOUND);
				return;
			}
		} else {
			domainName = User.DEFAULT_AUTH_DOMAIN;
		}

		List<User> users = daoFactory.getUserDao().getUsers(userLogins, domainName);
		if (users.size() != userLogins.size()) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_USERS);
			return;
		}

		rule.setDomainName(domainName);
		rule.setUsers(userLogins);
	}
}
