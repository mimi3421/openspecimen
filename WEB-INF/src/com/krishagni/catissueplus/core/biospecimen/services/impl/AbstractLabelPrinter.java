package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem.Status;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.domain.PrintRuleEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EventCode;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public abstract class AbstractLabelPrinter<T> implements LabelPrinter<T>, ApplicationListener<OpenSpecimenEvent> {
	//
	// format: <entity_type>_<yyyyMMddHHmm>_<unique_os_run_num>_<copy>.txt
	// E.g. specimen_201604040807_1_1.txt, specimen_201604040807_1_2.txt, visit_201604040807_1_1.txt etc
	//
	private static final Log logger = LogFactory.getLog(AbstractLabelPrinter.class);

	private static final String LABEL_FILENAME_FMT = "%s_%s_%d_%d.txt";

	private static final String TSTAMP_FMT = "yyyyMMddHHmm";

	private AtomicInteger uniqueNum = new AtomicInteger();

	protected List<? extends LabelPrintRule> rules = null;

	protected DaoFactory daoFactory;

	protected LabelTmplTokenRegistrar printLabelTokensRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setPrintLabelTokensRegistrar(LabelTmplTokenRegistrar printLabelTokensRegistrar) {
		this.printLabelTokensRegistrar = printLabelTokensRegistrar;
	}

	@Override
	public List<LabelTmplToken> getTokens() {
		return printLabelTokensRegistrar.getTokens();
	}

	@Override
	public LabelPrintJob print(List<PrintItem<T>> printItems) {
		try {
			if (rules == null) {
				synchronized (this) {
					if (rules == null) {
						loadRulesFromDb();
						if (rules == null) {
							return null;
						}
					}
				}
			}

			String ipAddr = AuthUtil.getRemoteAddr();
			User currentUser = AuthUtil.getCurrentUser();

			LabelPrintJob job = new LabelPrintJob();
			job.setSubmissionDate(Calendar.getInstance().getTime());
			job.setSubmittedBy(currentUser);
			job.setItemType(getItemType());

			List<Map<String, Object>> labelDataList = new ArrayList<>();
			for (PrintItem<T> printItem : printItems) {
				boolean found = false;
				T obj = printItem.getObject();
				for (LabelPrintRule rule : rules) {
					if (!isApplicableFor(rule, obj, currentUser, ipAddr)) {
						continue;
					}

					Map<String, String> labelDataItems = rule.getDataItems(printItem);

					LabelPrintJobItem item = new LabelPrintJobItem();
					item.setJob(job);
					item.setPrinterName(rule.getPrinterName());
					item.setItemLabel(getItemLabel(obj));
					item.setItemId(getItemId(obj));
					item.setCopies(printItem.getCopies());
					item.setStatus(LabelPrintJobItem.Status.QUEUED);
					item.setLabelType(rule.getLabelType());
					item.setData(new ObjectMapper().writeValueAsString(labelDataItems));
					item.setDataItems(labelDataItems);

					job.getItems().add(item);
					labelDataList.add(makeLabelData(item, rule, labelDataItems));

					found = true;
					break;
				}

				if (!found) {
					logger.warn("No print rule matched for the order item: " + getItemLabel(obj));
				}
			}

			if (job.getItems().isEmpty()) {
				return null;
			}

			generateCmdFiles(labelDataList);
			daoFactory.getLabelPrintJobDao().saveOrUpdate(job);
			return job;
		} catch (Exception e) {
			logger.error("Error printing distribution labels", e);
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public void onApplicationEvent(OpenSpecimenEvent event) {
		EventCode code = event.getEventCode();
		if (code != PrintRuleEvent.CREATED && code != PrintRuleEvent.UPDATED && code != PrintRuleEvent.DELETED) {
			return;
		}

		PrintRuleConfig ruleCfg = (PrintRuleConfig) event.getEventData();
		if (ruleCfg.getObjectType().equals(getObjectType())) {
			loadRulesFromDb();
		}
	}

	protected abstract boolean isApplicableFor(LabelPrintRule rule, T obj, User user, String ipAddr);

	protected abstract String getObjectType();

	protected abstract String getItemType();

	protected abstract String getItemLabel(T obj);

	protected abstract Long getItemId(T obj);

	protected void loadRulesFromDb() {
		try {
			logger.info("Loading print rules from database for: " + getObjectType());
			rules = daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType(getObjectType()))
				.stream().map(PrintRuleConfig::getRule)
				.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Error loading print rules for: " + getObjectType(), e);
			throw new RuntimeException("Error loading print rules for: " + getObjectType(), e);
		}
	}

	protected Map<String, Object> makeLabelData(LabelPrintJobItem item, LabelPrintRule rule, Map<String, String> dataItems) {
		Map<String, Object> labelData = new HashMap<>();
		labelData.put("jobItem", item);
		labelData.put("rule", rule);
		labelData.put("dataItems", dataItems);
		return labelData;
	}

	@SuppressWarnings("unchecked")
	protected void generateCmdFiles(List<Map<String, Object>> labelDataList) {
		for (Map<String, Object> labelData : labelDataList) {
			generateCmdFile(
				(LabelPrintJobItem)labelData.get("jobItem"),
				(LabelPrintRule)labelData.get("rule"),
				(Map<String, String>)labelData.get("dataItems"));
		}
	}

	private void generateCmdFile(LabelPrintJobItem jobItem, LabelPrintRule rule, Map<String, String> dataItems) {
		if (StringUtils.isBlank(rule.getCmdFilesDir()) || rule.getCmdFilesDir().trim().equals("*")) {
			return;
		}

		try {
			String content = null;
			switch (rule.getCmdFileFmt()) {
				case CSV:
					content = getCommaSeparatedValueFields(dataItems);
					break;

				case KEY_VALUE:
					content = getKeyValueFields(dataItems);
					break;
			}

			writeToFile(jobItem, rule, content);
			jobItem.setStatus(Status.QUEUED);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private String getCommaSeparatedValueFields(Map<String, String> dataItems) {
		return Utility.stringListToCsv(dataItems.values());
	}

	private String getKeyValueFields(Map<String, String> dataItems) {
		StringBuilder content = new StringBuilder();
		for (Map.Entry<String, String> dataItem : dataItems.entrySet()) {
			content.append(String.format("%s=%s\n", dataItem.getKey(), dataItem.getValue()));
		}

		if (!dataItems.isEmpty()) {
			content.deleteCharAt(content.length() - 1);
		}

		return content.toString();
	}

	private void writeToFile(LabelPrintJobItem item, LabelPrintRule rule, String content)
	throws IOException {
		String tstamp = new SimpleDateFormat(TSTAMP_FMT).format(item.getJob().getSubmissionDate());
		int labelCount = uniqueNum.incrementAndGet();

		for (int i = 0; i < item.getCopies(); ++i) {
			String filename = String.format(LABEL_FILENAME_FMT, item.getJob().getItemType(), tstamp, labelCount, (i + 1));
			FileUtils.write(new File(rule.getCmdFilesDir(), filename), content);
		}
	}
}
