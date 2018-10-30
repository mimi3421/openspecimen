package com.krishagni.catissueplus.core.de.services.impl;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.QueryService;

@Configurable
public class DefaultQueryExportProcessor implements QueryService.ExportProcessor {
	private static final String QUERY_EXPORTED_BY = "query_exported_by";

	private static final String QUERY_EXPORTED_ON = "query_exported_on";

	private static final String QUERY_CP = "query_cp";

	@Autowired
	private DaoFactory daoFactory;

	private Long cpId;

	public DefaultQueryExportProcessor(Long cpId) {
		this.cpId = cpId;
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public void headers(OutputStream out) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(msg(QUERY_EXPORTED_BY), AuthUtil.getCurrentUser().formattedName());
		headers.put(msg(QUERY_EXPORTED_ON), Utility.getDateTimeString(Calendar.getInstance().getTime()));
		if (cpId != null && cpId != -1L) {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp != null) {
				headers.put(msg(QUERY_CP), cp.getShortTitle());
			}
		}

		headers.put("", "");
		Utility.writeKeyValuesToCsv(out, headers);
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}
}