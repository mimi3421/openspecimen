package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.DistributionLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.AbstractLabelPrinter;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.domain.PrintRuleEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EventCode;
import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class DefaultDistributionLabelPrinter extends AbstractLabelPrinter<DistributionOrderItem> implements ApplicationListener<OpenSpecimenEvent> {

	private static final Log logger = LogFactory.getLog(DefaultDistributionLabelPrinter.class);

	private List<DistributionLabelPrintRule> rules = null;

	private DaoFactory daoFactory;

	private LabelTmplTokenRegistrar printLabelTokensRegistrar;

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
	public LabelPrintJob print(List<PrintItem<DistributionOrderItem>> printItems) {
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
			job.setItemType(DistributionOrderItem.getEntityName());

			List<Map<String, Object>> labelDataList = new ArrayList<>();
			for (PrintItem<DistributionOrderItem> printItem : printItems) {
				boolean found = false;
				DistributionOrderItem orderItem = printItem.getObject();
				for (DistributionLabelPrintRule rule : rules) {
					if (!rule.isApplicableFor(orderItem, currentUser, ipAddr)) {
						continue;
					}

					Map<String, String> labelDataItems = rule.getDataItems(printItem);

					LabelPrintJobItem item = new LabelPrintJobItem();
					item.setJob(job);
					item.setPrinterName(rule.getPrinterName());
					item.setItemLabel(orderItem.getId().toString());
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
					logger.warn("No print rule matched for the order item: " + orderItem.getLabel());
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
		if (code == PrintRuleEvent.CREATED || code == PrintRuleEvent.UPDATED || code == PrintRuleEvent.DELETED) {
			loadRulesFromDb();
		}
	}

	private void loadRulesFromDb() {
		try {
			logger.info("Loading distribution print rules from database ...");
			this.rules = daoFactory.getPrintRuleConfigDao()
				.getPrintRules(new PrintRuleConfigsListCriteria().objectType("ORDER_ITEM"))
				.stream().map(pr -> (DistributionLabelPrintRule) pr.getRule())
				.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Error loading distribution label print rules", e);
			throw new RuntimeException("Error loading distribution label print rules", e);
		}
	}
}
