package com.krishagni.catissueplus.core.administrative.services.impl;

import com.krishagni.catissueplus.core.administrative.events.DpRequirementDetail;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class DpRequirementImporter implements ObjectImporter<DpRequirementDetail, DpRequirementDetail> {
	private DistributionProtocolService dpSvc;

	public void setDpSvc(DistributionProtocolService dpSvc) {
		this.dpSvc = dpSvc;
	}

	@Override
	public ResponseEvent<DpRequirementDetail> importObject(RequestEvent<ImportObjectDetail<DpRequirementDetail>> req) {
		try {
			ImportObjectDetail<DpRequirementDetail> detail = req.getPayload();
			RequestEvent<DpRequirementDetail> dpReq = new RequestEvent<>(detail.getObject());

			ResponseEvent<DpRequirementDetail> resp;
			if (detail.isCreate()) {
				resp = dpSvc.createRequirement(dpReq);
			} else {
				resp = dpSvc.patchRequirement(dpReq);
			}

			return resp;
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}
