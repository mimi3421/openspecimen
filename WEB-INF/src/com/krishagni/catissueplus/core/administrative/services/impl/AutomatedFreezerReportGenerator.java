package com.krishagni.catissueplus.core.administrative.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;

@Configurable
public class AutomatedFreezerReportGenerator implements ScheduledTask {
	private final static Log logger = LogFactory.getLog(AutomatedFreezerReportGenerator.class);

	@Autowired
	private StorageContainerService storageContainerSvc;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			storageContainerSvc.generateAutoFreezerReport(new RequestEvent<>());
		} catch (Exception e) {
			logger.error("Error generating automated freezer report", e);
		}
	}
}
