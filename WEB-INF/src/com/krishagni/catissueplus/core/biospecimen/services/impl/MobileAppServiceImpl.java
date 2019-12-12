package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.RegistrationQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenAliquotsSpec;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.MobileAppService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DatePicker;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FormData;

@Configurable
public class MobileAppServiceImpl implements MobileAppService {

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private CollectionProtocolRegistrationService cprSvc;

	@Autowired
	private VisitService visitSvc;

	@Autowired
	private SpecimenService spmnSvc;

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> getCpDetail(RequestEvent<String> req) {
		try {
			String shortTitle = req.getPayload();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND, shortTitle);
			}

			AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

			CpWorkflowConfig.Workflow workflow = CpWorkflowTxnCache.getInstance()
				.getWorkflow(cp.getId(), "mobile-app");
			Map<String, Object> data = new HashMap<>();
			if (workflow != null && workflow.getData() != null) {
				data = workflow.getData();
			}

			Map<String, Object> result = new HashMap<>();
			result.put("id", cp.getId());
			result.put("shortTitle", cp.getShortTitle());
			result.put("pi", cp.getPrincipalInvestigator().formattedName());
			result.put("workflow", data);

			Map<String, Object> forms = (Map<String, Object>) data.get("forms");
			Set<String> formNames = new HashSet<>();
			formNames.addAll(getFormNames(forms, "registration"));
			formNames.addAll(getFormNames(forms, "specimen"));
			result.put("forms", getFormDefs(formNames));

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Container> getForm(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();
			Number cpId = (Number) input.get("cpId");

			String entity     = (String) input.get("entity");
			String viewForm   = (String) input.get("viewForm");
			return ResponseEvent.response(getForm(cpId.longValue(), entity, viewForm));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> saveFormData(RequestEvent<FormDataDetail> req) {
		try {
			FormDataDetail input = req.getPayload();
			FormData formData = input.getFormData();
			Map<String, Object> appData = formData.getAppData();
			if (appData == null || appData.isEmpty()) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "App data is null/empty");
			}

			String action = (String) appData.get("action");
			Number cpId = (Number) appData.get("cpId");

			Map<String, Object> fields = groupFields(formData.getFieldNameValueMap(true));
			fields.put("cpId", cpId.longValue());

			Map<String, Object> result = null;
			switch (action) {
				case "registerParticipant":
					CollectionProtocolRegistrationDetail cpr = toCpr(fields);
					cpr = saveOrUpdateCpr(cpr);
					result = getFormData(formData.getContainer(), cpr).getFieldValueMap();
					break;

				case "addVisit":
					VisitDetail visit = toVisit(appData, fields);
					visit = saveOrUpdateVisit(visit);
					result = getFormData(formData.getContainer(), visit).getFieldValueMap();
					break;

				case "addSpecimen":
					SpecimenDetail spmn = toSpecimen(appData, fields);
					spmn = saveOrUpdateSpecimen(spmn);
					result = getFormData(formData.getContainer(), spmn).getFieldValueMap();
					break;

				case "createAliquots":
					SpecimenAliquotsSpec spec = toAliquotSpec(appData, fields);
					List<SpecimenDetail> aliquots = createAliquots(spec);
					result = getFormData(formData.getContainer(), aliquots.get(0)).getFieldValueMap();
					break;
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> getFormData(RequestEvent<Map<String, String>> req) {
		try {
			Map<String, String> input = req.getPayload();
			String action = input.get("action");
			if (StringUtils.isBlank(action)) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Action is blank or empty!");
			}

			switch (action) {
				case "getParticipant":
					return ResponseEvent.response(getCpr(input));

				case "getSpecimen":
					return ResponseEvent.response(getSpecimen(input));

				default:
					return null;
			}
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<? extends SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req) {
		return spmnSvc.getSpecimens(req);
	}

	private CollectionProtocol getCp(Long cpId) {
		if (cpId == null) {
			throw OpenSpecimenException.userError(CpErrorCode.REQUIRED);
		}

		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
		}

		return cp;
	}

	private CollectionProtocolRegistrationDetail toCpr(Map<String, Object> fields) {
		return toObject(fields, CollectionProtocolRegistrationDetail.class);
	}

	private VisitDetail toVisit(Map<String, Object> appData, Map<String, Object> fields) {
		VisitDetail visit = toObject(fields, VisitDetail.class);
		Number cprId = (Number) appData.get("cprId");
		if (cprId != null) {
			visit.setCprId(cprId.longValue());
		}

		return visit;
	}

	private SpecimenDetail toSpecimen(Map<String, Object> appData, Map<String, Object> fields) {
		SpecimenDetail spmn = toObject(fields, SpecimenDetail.class);
		Number cprId = (Number) appData.get("cprId");
		if (cprId != null) {
			spmn.setCprId(cprId.longValue());
		}

		return spmn;
	}

	private SpecimenAliquotsSpec toAliquotSpec(Map<String, Object> appData, Map<String, Object> fields) {
		SpecimenAliquotsSpec aliquotsSpec = toObject(fields, SpecimenAliquotsSpec.class);

		Number parentId = (Number) appData.get("parentSpecimenId");
		if (parentId != null) {
			aliquotsSpec.setParentId(parentId.longValue());
		}

		return aliquotsSpec;
	}

	private <T> T toObject(Map<String, Object> fields, Class<T> klass) {
		return new ObjectMapper()
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.convertValue(fields, klass);
	}

	private Map<String, Object> groupFields(Map<String, Object> input) {
		Map<String, Map<String, Object>> groupedFields = new HashMap<>();
		Map<String, Object> result = new HashMap<>();

		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String[] fieldParts = entry.getKey().split("_", 2);
			if (fieldParts.length == 1) {
				result.put(fieldParts[0], entry.getValue());
			} else {
				Map<String, Object> fields = groupedFields.computeIfAbsent(fieldParts[0], (k) -> new HashMap<>());
				fields.put(fieldParts[1], entry.getValue());
			}
		}

		for (Map.Entry<String, Map<String, Object>> groupedField : groupedFields.entrySet()) {
			result.put(groupedField.getKey(), groupFields(groupedField.getValue()));
		}

		return result;
	}

	private Map<String, Object> getCpr(Map<String, String> input) {
		String cprIdStr = input.get("cprId");
		if (cprIdStr == null || cprIdStr.trim().isEmpty()) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "CPR ID is required");
		}

		Long cprId = null;
		try {
			cprId = Long.parseLong(cprIdStr);
		} catch (NumberFormatException nfe) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "CPR ID " + cprIdStr + " is not valid!");
		}

		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		CollectionProtocolRegistrationDetail cpr = ResponseEvent.unwrap(cprSvc.getRegistration(RequestEvent.wrap(crit)));
		Container form = getForm(cpr.getCpId(), "registration", "overview");
		return getFormData(form, cpr).getFieldValueMap();
	}

	private Map<String, Object> getSpecimen(Map<String, String> input) {
		String spmnIdStr = input.get("specimenId");
		if (spmnIdStr == null || spmnIdStr.trim().isEmpty()) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Specimen ID is required");
		}

		Long specimenId = null;
		try {
			specimenId = Long.parseLong(spmnIdStr);
		} catch (NumberFormatException nfe) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Specimen ID " + spmnIdStr + " is not valid!");
		}

		SpecimenQueryCriteria crit = new SpecimenQueryCriteria(specimenId);
		crit.setIncludeChildren(false);
		SpecimenDetail spmn = ResponseEvent.unwrap(spmnSvc.getSpecimen(RequestEvent.wrap(crit)));
		String prefix = spmn.getLineage().equals(Specimen.ALIQUOT) ? "aliquot": "primary";
		Container form = getForm(spmn.getCpId(), "specimen", prefix + "Overview");
		return getFormData(form, spmn).getFieldValueMap();
	}

	private Container getForm(Long cpId, String entity, String viewForm) {
		CollectionProtocol cp = getCp(cpId);
		AccessCtrlMgr.getInstance().ensureReadCpRights(cp);

		Map<String, Map<String, String>> forms = CpWorkflowTxnCache.getInstance().getValue(cpId, "mobile-app", "forms");
		if (forms == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "CP has no forms configured!");
		}

		Map<String, String> entityViews = forms.get(entity);
		if (entityViews == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "CP has no " + entity + " views configured!");
		}

		String formName = entityViews.get(viewForm);
		if (formName == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "CP has no " + entity + "/" + viewForm + " form configured!");
		}

		Container form = Container.getContainer(formName);
		if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formName);
		}

		return form;
	}

	private FormData getFormData(Container form, CollectionProtocolRegistrationDetail cpr) {
		Map<String, Object> appData = new HashMap<>();
		appData.put("cpId", cpr.getCpId());
		appData.put("title", cpr.getPpid());
		appData.put("cprId", cpr.getId());
		return getFormData(form, cpr, cpr.getId(), appData);
	}

	private FormData getFormData(Container form, VisitDetail visit) {
		Map<String, Object> appData = new HashMap<>();
		appData.put("cpId", visit.getCpId());
		appData.put("title", visit.getEventLabel());
		appData.put("cprId", visit.getCprId());
		return getFormData(form, visit, visit.getId(), appData);
	}

	private FormData getFormData(Container form, SpecimenDetail spmn) {
		Map<String, Object> appData = new HashMap<>();
		appData.put("cpId", spmn.getCpId());
		appData.put("title", spmn.getLabel());
		appData.put("cprId", spmn.getCprId());
		appData.put("visitId", spmn.getVisitId());
		appData.put("lineage", spmn.getLineage());
		return getFormData(form, spmn, spmn.getId(), appData);
	}

	private FormData getFormData(Container form, Object input, Long id, Map<String, Object> appData) {
		try {
			FormData formData = new FormData(form);
			formData.setRecordId(id);
			formData.setAppData(appData);

			PropertyUtilsBean propertyUtils = BeanUtilsBean2.getInstance().getPropertyUtils();
			for (Control ctrl : form.getOrderedControlList()) {
				String name = ctrl.getUserDefinedName();

				Object value = input;
				String[] nameParts = name.split("_");
				for (String part : nameParts) {
					try {
						value = propertyUtils.getProperty(value, part);
					} catch (NoSuchMethodException nsme) {
						value = null;
					}

					if (value == null) {
						break;
					}
				}

				if (value instanceof String && ctrl instanceof DatePicker) {
					DatePicker dp = (DatePicker) ctrl;
					try {
						if (dp.isDateTimeFmt()) {
							value = Utility.parseDateTimeString((String) value).getTime();
						} else {
							value = Utility.parseDateString((String) value).getTime();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				formData.addFieldValue(new ControlValue(ctrl, value));
			}

			return formData;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private Set<String> getFormNames(Map<String, Object> forms, String entity) {
		if (forms == null) {
			return Collections.emptySet();
		}

		Map<String, String> entityForms = (Map<String, String>)forms.get(entity);
		if (entityForms == null) {
			return Collections.emptySet();
		}

		return new HashSet<>(entityForms.values());
	}

	private List<Map<String, Object>> getFormDefs(Collection<String> names) {
		List<Map<String, Object>> formDefs = new ArrayList<>();
		for (String name : names) {
			Container form = Container.getContainer(name);
			if (form == null) {
				// TODO
				continue;
			}

			formDefs.add(form.getProps());
		}

		return formDefs;
	}

	private CollectionProtocolRegistrationDetail saveOrUpdateCpr(CollectionProtocolRegistrationDetail input) {
		if (input.getRegistrationDate() == null) {
			input.setRegistrationDate(Calendar.getInstance().getTime());
		}

		if (input.getParticipant() == null) {
			input.setParticipant(new ParticipantDetail());
		}

		if (input.getId() != null) {
			input = ResponseEvent.unwrap(cprSvc.updateRegistration(RequestEvent.wrap(input)));
		} else {
			input = ResponseEvent.unwrap(cprSvc.createRegistration(RequestEvent.wrap(input)));
		}

		return input;
	}

	private VisitDetail saveOrUpdateVisit(VisitDetail input) {
		if (input.getVisitDate() == null) {
			input.setVisitDate(Calendar.getInstance().getTime());
		}

		if (StringUtils.isBlank(input.getStatus())) {
			input.setStatus(Visit.VISIT_STATUS_COMPLETED);
		}

		if (input.getId() != null) {
			input = ResponseEvent.unwrap(visitSvc.patchVisit(RequestEvent.wrap(input)));
		} else {
			input = ResponseEvent.unwrap(visitSvc.addVisit(RequestEvent.wrap(input)));
		}

		return input;
	}

	private SpecimenDetail saveOrUpdateSpecimen(SpecimenDetail input) {
		if (input.getId() != null) {
			return ResponseEvent.unwrap(spmnSvc.updateSpecimen(RequestEvent.wrap(input)));
		}

		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(input.getCprId());
		if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND, input.getCprId());
		}

		Date collectionDate =  null;
		if (input.getCollectionEvent() != null) {
			collectionDate = input.getCollectionEvent().getTime();
		}

		if (collectionDate == null && input.getCreatedOn() != null) {
			collectionDate = input.getCreatedOn();
		}

		if (collectionDate == null) {
			collectionDate = Calendar.getInstance().getTime();
		}

		final Date visitDate = collectionDate;
		Visit visit = null;
		if (StringUtils.isNotBlank(input.getEventLabel())) {
			visit = cpr.getOrderedVisits().stream()
				.filter(v -> v.getCpEvent() != null && v.getCpEvent().getEventLabel().equals(input.getEventLabel()))
				.findFirst().orElse(null);
		} else {
			visit = cpr.getOrderedVisits().stream()
				.filter(v -> DateUtils.isSameDay(v.getVisitDate(), visitDate))
				.findFirst().orElse(null);
		}

		if (visit != null) {
			input.setVisitId(visit.getId());
		} else {
			VisitDetail visitDetail = new VisitDetail();
			visitDetail.setVisitDate(visitDate);
			visitDetail.setClinicalStatus(Specimen.NOT_SPECIFIED);
			visitDetail.setClinicalDiagnoses(Collections.singleton(Specimen.NOT_SPECIFIED));
			visitDetail.setCprId(cpr.getId());
			visitDetail.setEventLabel(input.getEventLabel());
			visitDetail.setCpId(cpr.getCollectionProtocol().getId());
			visitDetail.setCpShortTitle(cpr.getCollectionProtocol().getShortTitle());

			if (cpr.getSite() != null) {
				visitDetail.setSite(cpr.getSite().getName());
			} else {
				CollectionProtocol cp = cpr.getCollectionProtocol();
				List<String> cpSites = cp.getSites().stream()
					.map(cpSite -> cpSite.getSite().getName())
					.sorted(String::compareTo)
					.collect(Collectors.toList());
				visitDetail.setSite(cpSites.get(0));
			}

			visitDetail = ResponseEvent.unwrap(visitSvc.addVisit(RequestEvent.wrap(visitDetail)));
			input.setVisitId(visitDetail.getId());
		}

		if (StringUtils.isBlank(input.getReqCode()) && StringUtils.isBlank(input.getType())) {
			if (StringUtils.isBlank(input.getSpecimenClass())) {
				input.setType(FLUID_NS);
			} else {
				input.setType(input.getSpecimenClass() + " - " + NS);
			}
		}

		return ResponseEvent.unwrap(spmnSvc.createSpecimen(RequestEvent.wrap(input)));
	}

	private List<SpecimenDetail> createAliquots(SpecimenAliquotsSpec spec) {
		if (spec.getParentId() == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PARENT_REQUIRED);
		}

		Long parentId = spec.getParentId();
		Specimen parentSpmn = daoFactory.getSpecimenDao().getById(parentId);
		if (parentSpmn == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, parentId);
		}

		String derivedReqCode = spec.getDerivedReqCode();
		if (StringUtils.isNotBlank(derivedReqCode)) {
			parentId = createDerivedSpecimenIfAbsent(parentSpmn, derivedReqCode);
		}

		spec.setParentId(parentId);
		spec.setLinkToReqs(true);
		return ResponseEvent.unwrap(spmnSvc.createAliquots(RequestEvent.wrap(spec)));
	}

	private Long createDerivedSpecimenIfAbsent(Specimen parentSpmn, String derivedReqCode) {
		Specimen derivedSpmn = parentSpmn.getChildCollection().stream()
			.filter(c -> c.isDerivative() && c.getSpecimenRequirement() != null && derivedReqCode.equals(c.getSpecimenRequirement().getCode()))
			.findFirst()
			.orElse(null);
		if (derivedSpmn != null) {
			return derivedSpmn.getId();
		}

		SpecimenRequirement sr = parentSpmn.getSpecimenRequirement();
		if (sr == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid derived req code");
		}

		SpecimenRequirement derivedReq = sr.getChildSpecimenRequirements().stream()
			.filter(r -> r.isDerivative() && derivedReqCode.equals(r.getCode()))
			.findFirst()
			.orElse(null);
		if (derivedReq == null) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid derived req code");
		}

		SpecimenDetail derivedDetail = new SpecimenDetail();
		derivedDetail.setReqId(derivedReq.getId());
		derivedDetail.setParentId(parentSpmn.getId());
		derivedDetail = ResponseEvent.unwrap(spmnSvc.createDerivative(RequestEvent.wrap(derivedDetail)));
		return derivedDetail.getId();
	}



	private static final String FLUID_NS = "Fluid - Not Specified";

	private static final String NS = "Not Specified";
}
