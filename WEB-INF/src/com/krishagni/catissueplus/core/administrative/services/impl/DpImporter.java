package com.krishagni.catissueplus.core.administrative.services.impl;

import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class DpImporter implements ObjectImporter<DistributionProtocolDetail, DistributionProtocolDetail> {
	private DistributionProtocolService dpSvc;

	public void setDpSvc(DistributionProtocolService dpSvc) {
		this.dpSvc = dpSvc;
	}

	@Override
	public ResponseEvent<DistributionProtocolDetail> importObject(RequestEvent<ImportObjectDetail<DistributionProtocolDetail>> req) {
		try {
			ImportObjectDetail<DistributionProtocolDetail> detail = req.getPayload();
			RequestEvent<DistributionProtocolDetail> dpReq = new RequestEvent<>(detail.getObject());

			ResponseEvent<DistributionProtocolDetail> resp;
			if (detail.isCreate()) {
				resp = dpSvc.createDistributionProtocol(dpReq);
			} else {
				resp = dpSvc.patchDistributionProtocol(dpReq);
			}

			return resp;
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
