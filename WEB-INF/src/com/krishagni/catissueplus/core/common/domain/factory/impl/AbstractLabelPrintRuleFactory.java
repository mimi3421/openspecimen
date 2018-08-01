package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrintRuleFactory implements LabelPrintRuleFactory {
	protected DaoFactory daoFactory;

	protected LabelTmplTokenRegistrar printLabelTokensRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	@Override
	public LabelPrintRule createLabelPrintRule(Map<String, String> ruleDef) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		LabelPrintRule rule = fromRuleDef(ruleDef, ose);
		setLabelType(ruleDef, rule, ose);
		setLabelDesign(ruleDef, rule, ose);
		setDataTokens(ruleDef, rule, ose);
		setCmdFilesDir(ruleDef, rule, ose);
		setCmdFileFmt(ruleDef, rule, ose);
		setPrinterName(ruleDef, rule, ose);
		setIpAddressMatcher(ruleDef, rule, ose);
		setUsers(ruleDef, rule, ose);

		ose.checkAndThrow();
		return rule;
	}

	public abstract LabelPrintRule fromRuleDef(Map<String, String> ruleDef, OpenSpecimenException ose);

	private void setLabelType(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelType"))) {
			rule.setLabelType("Std");
			return;
		}

		rule.setLabelType(input.get("labelType"));
	}

	private void setLabelDesign(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelDesign"))) {
			return;
		}

		rule.setLabelDesign(input.get("labelDesign"));
	}

	private void setDataTokens(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
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

	private void setCmdFilesDir(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
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

	private void setCmdFileFmt(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
		String cmdFileFmt = input.get("cmdFileFmt");
		if (StringUtils.isBlank(cmdFileFmt)) {
			return;
		}

		if (LabelPrintRule.CmdFileFmt.get(cmdFileFmt) == null) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILE_FMT, cmdFileFmt);
		}

		rule.setCmdFileFmt(cmdFileFmt);
	}

	private void setPrinterName(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("printerName"))) {
			rule.setPrinterName("default");
			return;
		}

		rule.setPrinterName(input.get("printerName"));
	}

	private void setIpAddressMatcher(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
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

	private void setUsers(Map<String, String> input, LabelPrintRule rule, OpenSpecimenException ose) {
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
