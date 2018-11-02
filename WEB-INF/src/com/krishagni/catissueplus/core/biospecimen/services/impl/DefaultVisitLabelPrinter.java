package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem.Status;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class DefaultVisitLabelPrinter extends AbstractLabelPrinter<Visit> implements InitializingBean, ConfigChangeListener {
	private static final Log logger = LogFactory.getLog(DefaultVisitLabelPrinter.class);
	
	private ConfigurationService cfgSvc;
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	@Override
	protected boolean isApplicableFor(LabelPrintRule rule, Visit visit, User user, String ipAddr) {
		VisitLabelPrintRule visitLabelPrintRule = (VisitLabelPrintRule) rule;
		return visitLabelPrintRule.isApplicableFor(visit, user, ipAddr);
	}

	@Override
	protected String getObjectType() {
		return "VISIT";
	}

	@Override
	protected String getItemType() {
		return Visit.getEntityName();
	}

	@Override
	protected String getItemLabel(Visit visit) {
		return visit.getName();
	}

	@Override
	protected void loadRulesFromDb() {
		reloadRules();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.rules = null;
		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
	}
	
	@Override
	public void onConfigChange(String name, String value) {
		if (!name.equals(ConfigParams.VISIT_LABEL_PRINT_RULES)) {
			return;
		}
		
		this.rules = null;
	}		
		
	private void reloadRules() {
		FileDetail fileDetail = cfgSvc.getFileDetail(ConfigParams.MODULE, ConfigParams.VISIT_LABEL_PRINT_RULES);
		if (fileDetail == null || fileDetail.getFileIn() == null) {
			this.rules = null;
			return;
		}

		List<VisitLabelPrintRule> rules = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(fileDetail.getFileIn()));

			String ruleLine = null;
			while ((ruleLine = reader.readLine()) != null) {
				VisitLabelPrintRule rule = parseRule(ruleLine);
				if (rule == null) {
					continue;
				}
				
				rules.add(rule);
				logger.info(String.format("Adding rule [%s]", rule));
			}

			this.rules = rules;
		} catch (Exception e) {
			logger.error("Error loading rules from file: " + fileDetail.getFilename(), e);
		} finally {
			IOUtils.closeQuietly(fileDetail.getFileIn());
			IOUtils.closeQuietly(reader);
		}
	}
	
	//
	// Format of each rule
	// cp_short_title	visit_site	user_login	ip_address	label_type	label_tokens	label_design	printer_name	dir_path
	//
	private VisitLabelPrintRule parseRule(String ruleLine) {
		try {
			if (ruleLine.startsWith("#")) {
				return null;
			}
			
			String[] ruleLineFields = ruleLine.split("\\t");
			if (ruleLineFields.length != 10) {
				logger.error(String.format("Invalid rule [%s]. Expected variables: 10, Actual: [%d]", ruleLine, ruleLineFields.length));
				return null;
			}
			
			int idx = 0;
			VisitLabelPrintRule rule = new VisitLabelPrintRule();
			rule.setCpShortTitle(ruleLineFields[idx++]);
			rule.setVisitSite(ruleLineFields[idx++]);
			rule.setUserLogin(ruleLineFields[idx++]);
			
			if (!ruleLineFields[idx++].equals("*")) {
				rule.setIpAddressMatcher(new IpAddressMatcher(ruleLineFields[idx - 1]));
			}
			rule.setLabelType(ruleLineFields[idx++]);

			String[] labelTokens = ruleLineFields[idx++].split(",");
			boolean badTokens = false;			
			
			List<LabelTmplToken> tokens = new ArrayList<LabelTmplToken>();
			for (String labelToken : labelTokens) {
				LabelTmplToken token = printLabelTokensRegistrar.getToken(labelToken);
				if (token == null) {
					logger.error(String.format("Invalid rule [%s]. Unknown token: [%s]", ruleLine, labelToken));
					badTokens = true;
					break;
				}
				
				tokens.add(token);
			}
			
			if (badTokens) {
				return null;
			}
			
			rule.setDataTokens(tokens);
			rule.setLabelDesign(ruleLineFields[idx++]);
			rule.setPrinterName(ruleLineFields[idx++]);
			rule.setCmdFilesDir(ruleLineFields[idx++]);

			if (!ruleLineFields[idx++].equals("*")) {
				rule.setCmdFileFmt(ruleLineFields[idx - 1]);
			}

			return rule;
		} catch (Exception e) {
			logger.error("Error parsing rule: " + ruleLine, e);
		}
		
		return null;
	}
}
