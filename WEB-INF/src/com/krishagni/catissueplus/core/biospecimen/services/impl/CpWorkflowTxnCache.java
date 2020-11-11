package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;

@Configurable
public class CpWorkflowTxnCache {
	private final static CpWorkflowTxnCache instance = new CpWorkflowTxnCache();

	private ThreadLocal<Map<Long, CpWorkflowConfig>> cpWorkflows = new ThreadLocal<Map<Long, CpWorkflowConfig>>() {
		@Override
		protected Map<Long, CpWorkflowConfig> initialValue() {
			TransactionalThreadLocals.getInstance().register(this);
			return new HashMap<>();
		}
	};

	@Autowired
	private DaoFactory daoFactory;

	public static CpWorkflowTxnCache getInstance() {
		return instance;
	}

	public CpWorkflowConfig getWorkflows(Long cpId) {
		CpWorkflowConfig config = cpWorkflows.get().get(cpId);
		if (config == null) {
			config = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
			if (config == null) {
				config = new CpWorkflowConfig();
			}

			cpWorkflows.get().put(cpId, config);
		}

		return config;
	}

	public CpWorkflowConfig.Workflow getWorkflow(Long cpId, String name) {
		CpWorkflowConfig config = getWorkflows(cpId);
		return config.getWorkflows().get(name);
	}

	public  <T> T getValue(Long cpId, String wfName, String propName) {
		CpWorkflowConfig.Workflow workflow = getWorkflow(cpId, wfName);
		if (workflow == null || workflow.getData() == null || workflow.getData().isEmpty()) {
			return null;
		}

		return (T) workflow.getData().get(propName);
	}
}
