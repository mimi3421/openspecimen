package com.krishagni.catissueplus.core.common.domain.factory.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrintRuleFactory implements LabelPrintRuleFactory {
	private static final Pattern funArgs = Pattern.compile("^(.+?)\\((.+?)\\)$");

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
		return createLabelPrintRule(ruleDef, true);
	}

	@Override
	public LabelPrintRule createLabelPrintRule(Map<String, String> ruleDef, boolean failOnError) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		LabelPrintRule rule = fromRuleDef(ruleDef, failOnError, ose);
		setLabelType(ruleDef, failOnError, rule, ose);
		setLabelDesign(ruleDef, failOnError, rule, ose);
		setDataTokens(ruleDef, failOnError, rule, ose);
		setCmdFilesDir(ruleDef, failOnError, rule, ose);
		setCmdFileFmt(ruleDef, failOnError, rule, ose);
		setPrinterName(ruleDef, failOnError, rule, ose);
		setIpAddressMatcher(ruleDef, failOnError, rule, ose);
		setUsers(ruleDef, failOnError, rule, ose);

		ose.checkAndThrow();
		return rule;
	}

	public abstract LabelPrintRule fromRuleDef(Map<String, String> ruleDef, boolean failOnError, OpenSpecimenException ose);

	protected Pair<List<Long>, List<String>> getIdsAndNames(List<String> inputList) {
		List<Long> ids = new ArrayList<>();
		List<String> names = new ArrayList<>();

		for (String input : inputList) {
			if (StringUtils.isBlank(input)) {
				continue;
			}

			try {
				ids.add(Long.parseLong(input));
			} catch (NumberFormatException nfe) {
				names.add(input);
			}
		}

		return Pair.make(ids, names);
	}

	protected <T, K> List<T> getList(Function<List<K>, List<T>> getObjs, List<K> keys, Function<T, K> keyMapper, OpenSpecimenException ose, ErrorCode invalid) {
		if (CollectionUtils.isEmpty(keys)) {
			return Collections.emptyList();
		}

		List<T> objects = getObjs.apply(keys);
		if (ose != null && objects.size() != keys.size()) {
			Set<K> foundKeys = objects.stream().map(keyMapper).collect(Collectors.toSet());
			ose.addError(invalid, keys.stream().filter(k -> !foundKeys.contains(k)).collect(Collectors.toList()));
		}

		return objects;
	}

	private void setLabelType(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelType"))) {
			rule.setLabelType("Std");
			return;
		}

		rule.setLabelType(input.get("labelType"));
	}

	private void setLabelDesign(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("labelDesign"))) {
			return;
		}

		rule.setLabelDesign(input.get("labelDesign"));
	}

	private void setDataTokens(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("dataTokens"))) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKENS_REQ);
			return;
		}

		List<String> invalidTokenNames = new ArrayList<>();
		List<Pair<LabelTmplToken, List<String>>> dataTokens = new ArrayList<>();

		List<String> tokenNames = parseTokens(input.get("dataTokens"));
		for (String key : tokenNames) {
			Pair<String, List<String>> tokenArgs = parseFunctionToken(key);
			LabelTmplToken token = printLabelTokensRegistrar.getToken(tokenArgs.first());
			if (token == null) {
				invalidTokenNames.add(key);
			} else {
				dataTokens.add(Pair.make(token, tokenArgs.second()));
			}
		}

		if (failOnError && CollectionUtils.isNotEmpty(invalidTokenNames)) {
			ose.addError(PrintRuleConfigErrorCode.LABEL_TOKEN_NOT_FOUND, invalidTokenNames, invalidTokenNames.size());
		}

		rule.setDataTokens(dataTokens);
	}

	private void setCmdFilesDir(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
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

		if (failOnError && (!dirCreated || !dir.isDirectory())) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILES_DIR, dir.getAbsolutePath());
			return;
		}

		rule.setCmdFilesDir(dirPath);
	}

	private String getDefaultPrintLabelsDir() {
		return ConfigUtil.getInstance().getDataDir() + File.separator + "print-labels";
	}

	private void setCmdFileFmt(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		String cmdFileFmt = input.get("cmdFileFmt");
		if (StringUtils.isBlank(cmdFileFmt)) {
			return;
		}

		if (LabelPrintRule.CmdFileFmt.get(cmdFileFmt) == null) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_CMD_FILE_FMT, cmdFileFmt);
		}

		rule.setCmdFileFmt(cmdFileFmt);
	}

	private void setPrinterName(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.get("printerName"))) {
			rule.setPrinterName("default");
			return;
		}

		rule.setPrinterName(input.get("printerName"));
	}

	private void setIpAddressMatcher(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
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

	private void setUsers(Map<String, String> input, boolean failOnError, LabelPrintRule rule, OpenSpecimenException ose) {
		List<String> userLogins = Utility.csvToStringList(input.get("users"));
		if (userLogins.isEmpty()) {
			userLogins = Utility.csvToStringList(input.get("userLogin")); // backward compatibility
		}

		if (userLogins.isEmpty()) {
			return;
		}

		List<User> result = new ArrayList<>();
		Pair<List<Long>, List<String>> idsAndLoginNames = getIdsAndNames(userLogins);

		if (!idsAndLoginNames.first().isEmpty()) {
			List<User> users = getList(
				(ids) -> daoFactory.getUserDao().getByIds(ids),
				idsAndLoginNames.first(), (u) -> u.getId(),
				failOnError ? ose : null, failOnError ? PrintRuleConfigErrorCode.INVALID_USERS : null);
			result.addAll(users);
		}

		if (!idsAndLoginNames.second().isEmpty()) {
			List<User> users = getList(
				(loginNames) -> daoFactory.getUserDao().getUsers(loginNames, null),
				idsAndLoginNames.second(), (u) -> u.getLoginName(),
				failOnError ? ose : null, failOnError ? PrintRuleConfigErrorCode.INVALID_USERS : null);
			result.addAll(users);
		}

		rule.setUsers(result);
	}

	private List<String> parseTokens(String input) {
		return parseCsv(input);
	}

	private Pair<String, List<String>> parseFunctionToken(String tokenStr) {
		Matcher matcher = funArgs.matcher(tokenStr);
		if (matcher.find()) {
			return Pair.make(matcher.group(1), parseCsv(matcher.group(2)));
		} else {
			return Pair.make(tokenStr, new ArrayList<>());
		}
	}

	private List<String> parseCsv(String args) {
		if (args == null || args.trim().isEmpty()) {
			return Collections.emptyList();
		}

		List<String> result = new ArrayList<>();
		StringBuilder arg = new StringBuilder();
		boolean insideQuote = false;
		int parenCnt = 0;
		for (int i = 0; i < args.trim().length(); ++i) {
			char ch = args.charAt(i);
			if (ch == ',' && parenCnt == 0 && !insideQuote) {
				result.add(arg.toString().trim());
				arg = new StringBuilder();
			} else {
				arg.append(ch);

				if (!insideQuote && ch == '(') {
					++parenCnt;
				} else if (!insideQuote && ch == ')') {
					--parenCnt;
				} else if (parenCnt == 0 && ch == '"') {
					insideQuote = !insideQuote;
				}
			}
		}

		if (!arg.toString().trim().isEmpty()) {
			result.add(arg.toString().trim());
		}

		return result;
	}
}
