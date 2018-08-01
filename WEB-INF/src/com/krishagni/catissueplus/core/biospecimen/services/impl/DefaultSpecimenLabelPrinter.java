package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenLabelPrintRule;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem.Status;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.domain.PrintRuleEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EventCode;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.service.ChangeLogService;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;


public class DefaultSpecimenLabelPrinter extends AbstractLabelPrinter<Specimen> implements InitializingBean, ApplicationListener<OpenSpecimenEvent> {
	private static final Log logger = LogFactory.getLog(DefaultSpecimenLabelPrinter.class);

	private List<SpecimenLabelPrintRule> rules = null;
	
	private DaoFactory daoFactory;
	
	private ConfigurationService cfgSvc;

	private LabelTmplTokenRegistrar printLabelTokensRegistrar;
	
	private ChangeLogService changeLogSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	public void setChangeLogSvc(ChangeLogService changeLogSvc) {
		this.changeLogSvc = changeLogSvc;
	}

	@Override
	public List<LabelTmplToken> getTokens() {
		return printLabelTokensRegistrar.getTokens();
	}

	@Override
	public LabelPrintJob print(List<PrintItem<Specimen>> printItems) {		
		try {
			if (rules == null) {
				synchronized (this) {
					if (rules == null) {
						loadRulesFromDb();
					}
				}
			}

			String ipAddr = AuthUtil.getRemoteAddr();
			User currentUser = AuthUtil.getCurrentUser();
			
			LabelPrintJob job = new LabelPrintJob();
			job.setSubmissionDate(Calendar.getInstance().getTime());
			job.setSubmittedBy(currentUser);
			job.setItemType(Specimen.getEntityName());

			List<Map<String, Object>> labelDataList = new ArrayList<>();
			for (PrintItem<Specimen> printItem : printItems) {				
				boolean found = false;
				Specimen specimen = printItem.getObject();
				for (SpecimenLabelPrintRule rule : rules) {
					if (!rule.isApplicableFor(specimen, currentUser, ipAddr)) {
						continue;
					}
					
					Map<String, String> labelDataItems = rule.getDataItems(printItem);

					LabelPrintJobItem item = new LabelPrintJobItem();
					item.setJob(job);
					item.setPrinterName(rule.getPrinterName());
					item.setItemLabel(specimen.getLabel());
					item.setCopies(printItem.getCopies());
					item.setStatus(Status.QUEUED);
					item.setLabelType(rule.getLabelType());
					item.setData(new ObjectMapper().writeValueAsString(labelDataItems));
					item.setDataItems(labelDataItems);

					job.getItems().add(item);
					labelDataList.add(makeLabelData(item, rule, labelDataItems));

					found = true;
					break;
				}

				if (!found) {
					logger.warn("No print rule matched specimen: " + specimen.getLabel());
				}
			}
			
			if (job.getItems().isEmpty()) {
				return null;				
			}
			
			generateCmdFiles(labelDataList);
			daoFactory.getLabelPrintJobDao().saveOrUpdate(job);			
			return job;
		} catch (Exception e) {
			logger.error("Error printing specimen labels", e);
			throw OpenSpecimenException.serverError(e);			
		}
	}	

	@Override
	@PlusTransactional
	public void afterPropertiesSet()
	throws Exception {
		boolean dbMigrationDone = changeLogSvc.doesChangeLogExists(PR_MIGRATION_ID, PR_MIGRATION_AUTHOR, PR_MIGRATION_FILE);
		if (!dbMigrationDone && migrateRulesToDb()) {
			changeLogSvc.insertChangeLog(PR_MIGRATION_ID, PR_MIGRATION_AUTHOR, PR_MIGRATION_FILE);
		}

		removePrintRulesSetting();
		cfgSvc.registerChangeListener(ConfigParams.MODULE, (name, value) -> {
			if (StringUtils.isBlank(name)) {
				removePrintRulesSetting();
			}
		});
	}

	@Override
	public void onApplicationEvent(OpenSpecimenEvent event) {
		EventCode code = event.getEventCode();
		if (code == PrintRuleEvent.CREATED || code == PrintRuleEvent.UPDATED || code == PrintRuleEvent.DELETED) {
			loadRulesFromDb();
		}
	}

	private boolean migrateRulesToDb() {
		FileDetail fileDetail = cfgSvc.getFileDetail(ConfigParams.MODULE, ConfigParams.SPECIMEN_LABEL_PRINT_RULES);
		if (fileDetail == null || fileDetail.getFileIn() == null) {
			return true;
		}

		List<SpecimenLabelPrintRule> rules = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(fileDetail.getFileIn()));

			String ruleLine = null;
			while ((ruleLine = reader.readLine()) != null) {
				SpecimenLabelPrintRule rule = parseRule(ruleLine);
				if (rule == null) {
					continue;
				}

				rules.add(rule);
				logger.info(String.format("Adding print rule: [%s]", rule));
			}

			saveToDb(rules);
			return true;
		} catch (Exception e) {
			logger.error("Error migrating print rules from file: " + fileDetail.getFilename(), e);
			return false;
		} finally {
			IOUtils.closeQuietly(fileDetail.getFileIn());
			IOUtils.closeQuietly(reader);
		}
	}

	//
	// Format of each rule
	// 	cp_short_title	visit_site	specimen_class	specimen_type
	//	user_login	ip_address	label_type	label_tokens	label_design
	//	printer_name	dir_path
	//
	private SpecimenLabelPrintRule parseRule(String ruleLine) {
		if (ruleLine.startsWith("#")) {
			return null;
		}

		String[] ruleLineFields = ruleLine.split("\\t");
		if (ruleLineFields.length < 12 || ruleLineFields.length > 13) {
			logger.error(String.format("Invalid rule [%s]. Expected variables: 12/13, Actual: [%d]", ruleLine, ruleLineFields.length));
			return null;
		}

		int idx = 0;
		SpecimenLabelPrintRule rule = new SpecimenLabelPrintRule();
		rule.setCps(Stream.of(ruleLineFields[idx++].split(",")).collect(Collectors.toList()));
		rule.setVisitSite(ruleLineFields[idx++]);
		rule.setSpecimenClass(ruleLineFields[idx++]);
		rule.setSpecimenType(ruleLineFields[idx++]);
		rule.setUsers(Stream.of(ruleLineFields[idx++].split(",")).collect(Collectors.toList()));

		if (!ruleLineFields[idx++].equals("*")) {
			rule.setIpAddressMatcher(new IpAddressMatcher(ruleLineFields[idx - 1]));
		}
		rule.setLabelType(ruleLineFields[idx++]);

		List<LabelTmplToken> tokens = new ArrayList<>();
		for (String labelToken : ruleLineFields[idx++].split(",")) {
			LabelTmplToken token = printLabelTokensRegistrar.getToken(labelToken);
			if (token == null) {
				String errorMsg = String.format("Invalid rule [%s]. Unknown token: [%s]", ruleLine, labelToken);
				throw new IllegalArgumentException(errorMsg);
			}

			tokens.add(token);
		}

		rule.setDataTokens(tokens);
		rule.setLabelDesign(ruleLineFields[idx++]);
		rule.setPrinterName(ruleLineFields[idx++]);
		rule.setCmdFilesDir(ruleLineFields[idx++]);

		if (!ruleLineFields[idx++].equals("*")) {
			rule.setCmdFileFmt(ruleLineFields[idx - 1]);
		}

		rule.setLineage(ruleLineFields.length > 12 ? ruleLineFields[idx++] : "*");
		return rule;
	}

	private void saveToDb(List<SpecimenLabelPrintRule> rules) {
		User systemUser = daoFactory.getUserDao().getSystemUser();
		int ruleIdx = 0;
		for (SpecimenLabelPrintRule rule : rules) {
			PrintRuleConfig ruleCfg = getPrintRuleConfig(rule, systemUser, ++ruleIdx);
			daoFactory.getPrintRuleConfigDao().saveOrUpdate(ruleCfg);
		}
	}

	private PrintRuleConfig getPrintRuleConfig(SpecimenLabelPrintRule rule, User systemUser, int ruleIdx) {
		PrintRuleConfig ruleCfg = new PrintRuleConfig();
		ruleCfg.setObjectType("SPECIMEN");
		ruleCfg.setRule(replaceWildcardsWithNull(rule));
		ruleCfg.setUpdatedBy(systemUser);
		ruleCfg.setUpdatedOn(Calendar.getInstance().getTime());
		ruleCfg.setActivityStatus("Active");
		ruleCfg.setDescription("Print rule " + ruleIdx);
		return ruleCfg;
	}

	private SpecimenLabelPrintRule replaceWildcardsWithNull(SpecimenLabelPrintRule rule) {
		rule.setCps(replaceWildcardWithNull(rule.getCps()));
		rule.setVisitSite(replaceWildcardWithNull(rule.getVisitSite()));
		rule.setLineage(replaceWildcardWithNull(rule.getLineage()));
		rule.setSpecimenClass(replaceWildcardWithNull(rule.getSpecimenClass()));
		rule.setSpecimenType(replaceWildcardWithNull(rule.getSpecimenType()));
		rule.setUsers(replaceWildcardWithNull(rule.getUsers()));
		rule.setLabelType(replaceWildcardWithNull(rule.getLabelType()));
		rule.setLabelDesign(replaceWildcardWithNull(rule.getLabelDesign()));
		rule.setPrinterName(replaceWildcardWithNull(rule.getPrinterName()));
		return rule;
	}

	private String replaceWildcardWithNull(String input) {
		return StringUtils.equals(input, "*") ? null : input;
	}

	private List<String> replaceWildcardWithNull(List<String> input) {
		if (input == null) {
			return null;
		}

		return input.stream().filter(e -> e != null && !e.equals("*")).collect(Collectors.toList());
	}

	private void loadRulesFromDb() {
		try {
			logger.info("Loading print rules from database ...");
			this.rules = daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType("SPECIMEN"))
				.stream().map(pr -> (SpecimenLabelPrintRule)pr.getRule())
				.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Error loading specimen label print rules", e);
			throw new RuntimeException("Error loading specimen label print rules", e);
		}
	}

	//
	// TODO: remove the config from database in v4.3
	//
	private void removePrintRulesSetting() {
		cfgSvc.removeSetting(ConfigParams.MODULE, ConfigParams.SPECIMEN_LABEL_PRINT_RULES);
	}

	private static final String PR_MIGRATION_ID = "Migration of specimen print rules to DB";

	private static final String PR_MIGRATION_AUTHOR = "$system";

	private static final String PR_MIGRATION_FILE = "specimen-print-rules.csv";
}
