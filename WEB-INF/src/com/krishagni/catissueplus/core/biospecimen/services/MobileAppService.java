package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;

import edu.common.dynamicextensions.domain.nui.Container;

public interface MobileAppService {
	ResponseEvent<Map<String, Object>> getCpDetail(RequestEvent<String> req);

	ResponseEvent<Container> getForm(RequestEvent<Map<String, Object>> req);

	ResponseEvent<Map<String, Object>> saveFormData(RequestEvent<FormDataDetail> req);

	ResponseEvent<Map<String, Object>> getFormData(RequestEvent<Map<String, String>> req);

	ResponseEvent<List<? extends SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<Map<String, Object>> uploadData(RequestEvent<Map<String, Object>> req);
}
