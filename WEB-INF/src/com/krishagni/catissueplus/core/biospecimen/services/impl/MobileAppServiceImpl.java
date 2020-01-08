package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
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
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.MobileAppService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.MobileUploadJob;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.MobileUploadJobDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.MobileUploadJobsListCriteria;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.CsvException;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.de.services.impl.ExtensionSchemaBuilder;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.services.ObjectReader;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DatePicker;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.nutility.IoUtil;
import edu.common.dynamicextensions.util.ZipUtility;
import krishagni.catissueplus.beans.FormContextBean;

@Configurable
public class MobileAppServiceImpl implements MobileAppService, InitializingBean {
	private static final Log logger = LogFactory.getLog(MobileAppServiceImpl.class);

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private CollectionProtocolRegistrationService cprSvc;

	@Autowired
	private VisitService visitSvc;

	@Autowired
	private SpecimenService spmnSvc;

	@Autowired
	private FormService formSvc;

	@Autowired
	private FormDao formDao;

	@Autowired
	private ThreadPoolTaskExecutor executor;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private PlatformTransactionManager txnMgr;

	private TransactionTemplate newTxnTmpl;

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

			CpWorkflowConfig.Workflow workflow = CpWorkflowTxnCache.getInstance().getWorkflow(cp.getId(), "mobile-app");
			Map<String, Object> data = new HashMap<>();
			if (workflow != null && workflow.getData() != null) {
				data = workflow.getData();
			}

			Map<String, Object> result = new HashMap<>();
			result.put("id", cp.getId());
			result.put("shortTitle", cp.getShortTitle());
			result.put("pi", cp.getPrincipalInvestigator().formattedName());
			result.put("workflow", data);

			Set<String> formNames = new HashSet<>();

			Map<String, Object> forms = (Map<String, Object>) data.get("forms");
			formNames.addAll(getFormNames(forms, "registration"));
			formNames.addAll(getFormNames(forms, "specimen"));

			Map<String, Object> addlForms = (Map<String, Object>) data.get("additionalForms");
			formNames.addAll(getAdditionalFormNames(addlForms, "registration"));

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
	public ResponseEvent<List<FormSummary>> getForms(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();
			Number cpId = (Number) input.get("cpId");
			if (cpId == null) {
				return ResponseEvent.userError(CpErrorCode.REQUIRED);
			}

			String entity = (String) input.get("entity");
			Map<String, Object> annotations = CpWorkflowTxnCache.getInstance().getValue(cpId.longValue(), "mobile-app", "additionalForms");
			if (annotations == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<String> names = (List<String>) annotations.get(entity);
			if (names == null || names.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			FormListCriteria crit = new FormListCriteria().names(names).cpIds(Arrays.asList(-1L, cpId.longValue()));
			switch (entity) {
				case "registration":
					crit.entityTypes(Arrays.asList("CommonParticipant", "Participant"));
					break;

				case "visit":
					crit.entityTypes(Collections.singletonList("SpecimenCollectionGroup"));
					break;

				case "specimen":
					crit.entityTypes(Collections.singletonList("Specimen"));
					break;
			}

			return ResponseEvent.response(formDao.getEntityForms(crit));
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

				case "saveFormData":
					result = saveFormData(cpId.longValue(), formData).getFieldValueMap();
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

	@PlusTransactional
	@Override
	public ResponseEvent<List<FormRecordSummary>> getFormRecords(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();

			Number objectId = (Number) input.get("objectId");
			String entity = (String) input.get("entity");
			if (!"registration".equals(entity)) {
				return ResponseEvent.response(Collections.emptyList());
			}

			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId.longValue());
			if (cpr == null) {
				return ResponseEvent.userError(CprErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);

			Long cpId = cpr.getCollectionProtocol().getId();
			Map<String, Object> annotations = CpWorkflowTxnCache.getInstance().getValue(cpId, "mobile-app", "additionalForms");
			if (annotations == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<String> names = (List<String>) annotations.get(entity);
			if (names == null || names.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<FormRecordSummary> records = getFormRecords("Participant", cpr.getId(), names);
			records.addAll(getFormRecords("CommonParticipant", cpr.getParticipant().getId(), names));
			records.sort(Comparator.comparing(FormRecordSummary::getUpdateTime).reversed());
			return ResponseEvent.response(records);
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

	@Override
	@PlusTransactional
	public ResponseEvent<List<MobileUploadJobDetail>> getUploadJobs(RequestEvent<MobileUploadJobsListCriteria> req) {
		try {
			MobileUploadJobsListCriteria crit = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				if (AuthUtil.isInstituteAdmin()) {
					crit.instituteId(AuthUtil.getCurrentUserInstitute().getId());
				} else {
					crit.userId(AuthUtil.getCurrentUser().getId());
				}
			}

			List<MobileUploadJob> jobs = daoFactory.getMobileUploadJobDao().getJobs(crit);
			return ResponseEvent.response(MobileUploadJobDetail.from(jobs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<MobileUploadJobDetail> getUploadJob(RequestEvent<Long> req) {
		try {
			MobileUploadJob job = getJob(req.getPayload());
			return ResponseEvent.response(MobileUploadJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> getUploadJobReport(RequestEvent<Long> req) {
		try {
			MobileUploadJob job = getJob(req.getPayload());
			return ResponseEvent.response(new File(job.getWorkingDir(), "output.zip"));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> uploadData(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();
			Long cpId = (Long) input.get("cpId");
			if (cpId == null) {
				throw OpenSpecimenException.userError(CpErrorCode.REQUIRED);
			}

			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
			}

			MobileUploadJob job = createJob(cp);
			inflateInputZip(job, (File) input.get("file"));
			executor.submit(
				() -> {
					try {
						AuthUtil.setCurrentUser(job.getCreatedBy());
						newTxnTmpl.execute(
							new TransactionCallback<Void>() {
								@Override
								public Void doInTransaction(TransactionStatus txnStatus) {
									importData(job);
									if (job.getStatus() != MobileUploadJob.Status.COMPLETED) {
										txnStatus.setRollbackOnly();
									}

									return null;
								}
							}
						);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						AuthUtil.clearCurrentUser();
					}
				}
			);

			return ResponseEvent.response(Collections.singletonMap("jobId", job.getId()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.newTxnTmpl = new TransactionTemplate(txnMgr);
		this.newTxnTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

		String ppid = (String) appData.get("ppid");
		if (ppid != null) {
			spmn.setPpid(ppid);
		}

		return spmn;
	}

	private SpecimenAliquotsSpec toAliquotSpec(Map<String, Object> appData, Map<String, Object> fields) {
		SpecimenAliquotsSpec aliquotsSpec = toObject(fields, SpecimenAliquotsSpec.class);
		aliquotsSpec.setCpShortTitle((String) appData.get("cpShortTitle"));

		Number parentId = (Number) appData.get("parentSpecimenId");
		if (parentId != null) {
			aliquotsSpec.setParentId(parentId.longValue());
		}

		String parentLabel = (String) appData.get("parentLabel");
		if (parentLabel != null) {
			aliquotsSpec.setParentLabel(parentLabel);
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

	private Set<String> getAdditionalFormNames(Map<String, Object> addlForms, String entity) {
		if (addlForms == null) {
			return Collections.emptySet();
		}

		List<String> formNames = (List<String>) addlForms.get(entity);
		if (formNames == null) {
			return Collections.emptySet();
		}

		return new HashSet<>(formNames);
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

		CollectionProtocolRegistration cpr = null;
		Object key = null;
		if (input.getCprId() != null) {
			cpr = daoFactory.getCprDao().getById(input.getCprId());
			key = input.getCprId();
		} else if (input.getCpId() != null && StringUtils.isNotBlank(input.getPpid())) {
			cpr = daoFactory.getCprDao().getCprByPpid(input.getCpId(), input.getPpid());
			key = input.getPpid();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CprErrorCode.PPID_REQUIRED);
		} else if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND, key);
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
		Object key = null;
		Specimen parentSpmn = null;
		if (spec.getParentId() != null) {
			parentSpmn = daoFactory.getSpecimenDao().getById(spec.getParentId());
			key = spec.getParentId();
		} else if (StringUtils.isNotBlank(spec.getCpShortTitle()) && StringUtils.isNotBlank(spec.getParentLabel())) {
			parentSpmn = daoFactory.getSpecimenDao().getByLabelAndCp(spec.getCpShortTitle(), spec.getParentLabel());
			key = spec.getParentLabel();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PARENT_REQUIRED);
		} else if (parentSpmn == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, key);
		}

		Long parentId = parentSpmn.getId();
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

	private FormData saveFormData(Long cpId, FormData formData) {
		FormDataDetail detail = new FormDataDetail();
		detail.setFormId(formData.getContainer().getId());
		detail.setFormData(formData);
		detail.setRecordId(formData.getRecordId());

		Container form = formData.getContainer();
		Map<String, Object> appData = formData.getAppData();

		if (!appData.containsKey("formCtxtId")) {
			String entityType = (String) appData.get("entityType");

			List<String> entities = null;
			if ("registration".equals(entityType)) {
				entities = Arrays.asList("CommonParticipant", "Participant");
			}

			if (entities != null) {
				FormContextBean fc = formDao.getFormContext(form.getId(), cpId, entities);
				if (fc == null) {
					throw new IllegalArgumentException(
						"Form " + form.getCaption() + " is not associated to the CP " + cpId + " at " + entityType
					);
				}

				appData.put("formCtxtId", fc.getIdentifier());
			}
		}

		detail = ResponseEvent.unwrap(formSvc.saveFormData(RequestEvent.wrap(detail)));
		return detail.getFormData();
	}

	private List<FormRecordSummary> getFormRecords(String entityType, Long objectId, List<String> formNames) {
		List<Object[]> rows = sessionFactory.getCurrentSession().createSQLQuery(GET_RECORDS_SQL)
			.addScalar("record_id", LongType.INSTANCE)
			.addScalar("user_id", LongType.INSTANCE)
			.addScalar("first_name", StringType.INSTANCE)
			.addScalar("last_name", StringType.INSTANCE)
			.addScalar("update_time", TimestampType.INSTANCE)
			.addScalar("form_ctxt_id", LongType.INSTANCE)
			.addScalar("entity_type", StringType.INSTANCE)
			.addScalar("form_id", LongType.INSTANCE)
			.addScalar("caption", StringType.INSTANCE)
			.setParameter("entityType", entityType)
			.setParameter("objectId", objectId)
			.setParameterList("formNames", formNames)
			.list();

		List<FormRecordSummary> records = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			FormRecordSummary record = new FormRecordSummary();
			record.setRecordId((Long) row[++idx]);

			UserSummary user = new UserSummary();
			user.setId((Long) row[++idx]);
			user.setFirstName((String) row[++idx]);
			user.setLastName((String) row[++idx]);
			record.setUser(user);
			record.setUpdateTime((Date) row[++idx]);
			record.setFcId((Long) row[++idx]);
			record.setEntityType((String) row[++idx]);
			record.setFormId((Long) row[++idx]);
			record.setFormCaption((String) row[++idx]);
			records.add(record);
		}

		return records;
	}

	//
	// import routines
	//

	private MobileUploadJob getJob(Long jobId) {
		MobileUploadJob job = daoFactory.getMobileUploadJobDao().getById(jobId);
		if (job == null) {
			throw  OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid job ID: " + jobId);
		}

		if (AuthUtil.isAdmin()) {
			return job;
		}

		if (AuthUtil.isInstituteAdmin()) {
			if (!AuthUtil.getCurrentUserInstitute().equals(job.getCreatedBy().getInstitute())) {
				throw  OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		} else {
			if (!AuthUtil.getCurrentUser().equals(job.getCreatedBy())) {
				throw  OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}

		return job;
	}

	private MobileUploadJob createJob(CollectionProtocol cp) {
		MobileUploadJob job = new MobileUploadJob();
		job.setCp(cp);
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setStatus(MobileUploadJob.Status.QUEUED);
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setTotalRecords(0L);
		job.setFailedRecords(0L);
		daoFactory.getMobileUploadJobDao().saveOrUpdate(job, true);

		job.getInputDir().mkdirs();
		job.getOutputDir().mkdirs();
		return job;
	}

	private void saveJob(MobileUploadJob job) {
		if (job.getStatus() != MobileUploadJob.Status.IN_PROGRESS) {
			job.setEndTime(Calendar.getInstance().getTime());
		}

		newTxnTmpl.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				daoFactory.getMobileUploadJobDao().saveOrUpdate(job);
				return null;
			}
		});
	}

	private void saveJob(MobileUploadJob job, long totalRecords, long failedRecords) {
		job.setTotalRecords(totalRecords);
		job.setFailedRecords(failedRecords);
		saveJob(job);
	}

	private void inflateInputZip(MobileUploadJob job, File inputFile)
	throws IOException {
		FileUtils.moveToDirectory(inputFile, job.getWorkingDir(), true);

		inputFile = new File(job.getWorkingDir(), inputFile.getName());
		ZipUtility.extractZipToDestination(inputFile.getAbsolutePath(), job.getInputDir().getAbsolutePath());
		inputFile.renameTo(new File(inputFile.getParentFile(), "input.zip"));
	}

	private void importData(MobileUploadJob job) {
		job.setStatus(MobileUploadJob.Status.IN_PROGRESS);
		saveJob(job);

		try {
			importParticipants(job);
			importPrimarySpecimens(job);
			importAliquots(job);
		} catch (Exception e) {
			job.setStatus(MobileUploadJob.Status.FAILED);
			logger.error("Error importing the mobile offline data", e);
		} finally {
			if (job.getStatus() == MobileUploadJob.Status.IN_PROGRESS) {
				if (job.getFailedRecords() > 0) {
					job.setStatus(MobileUploadJob.Status.FAILED);
				} else {
					job.setStatus(MobileUploadJob.Status.COMPLETED);
				}
			}

			saveJob(job);
			sendJobStatusNotification(job);
			tidyFiles(job);
		}
	}

	private void importParticipants(MobileUploadJob job) {
		importRecords(
			job,
			new SchemaParams("registration", "dataEntry", "registerParticipants", "participants.csv"),
			(record) -> {
				CollectionProtocolRegistrationDetail input = toCpr(record);
				input.setCpId(job.getCp().getId());
				importParticipant(input);
				return null;
			}
		);
	}

	private void importParticipant(CollectionProtocolRegistrationDetail input) {
		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getCprByPpid(input.getCpId(), input.getPpid());
		if (cpr != null) {
			input.setId(cpr.getId());
		}

		saveOrUpdateCpr(input);
	}

	private void importPrimarySpecimens(MobileUploadJob job) {
		importRecords(
			job,
			new SchemaParams("specimen", "primaryDataEntry", "addSpecimen", "primary-specimens.csv"),
			(record) -> {
				Map<String, Object> appData = new HashMap<>();
				appData.put("cpId", job.getCp().getId());
				appData.put("cpShortTitle", job.getCp().getShortTitle());
				appData.put("ppid", record.get("ppid"));

				SpecimenDetail spmn = toSpecimen(appData, record);
				spmn.setCpId(job.getCp().getId());
				saveOrUpdateSpecimen(spmn);
				return null;
			}
		);
	}

	private void importAliquots(MobileUploadJob job) {
		importRecords(
			job,
			new SchemaParams("specimen", "aliquotDataEntry", "createAliquots", "aliquots.csv"),
			(record) -> {
				Map<String, Object> appData = new HashMap<>();
				appData.put("cpId", job.getCp().getId());
				appData.put("cpShortTitle", job.getCp().getShortTitle());
				appData.put("ppid", record.get("ppid"));
				appData.put("parentLabel", record.get("parentLabel"));

				SpecimenAliquotsSpec spec = toAliquotSpec(appData, record);
				spec.setCpShortTitle(job.getCp().getShortTitle());
				createAliquots(spec);
				return null;
			}
		);
	}

	private void importRecords(MobileUploadJob job, SchemaParams params, Function<Map<String, Object>, String> importer) {
		ObjectSchema schema = getSchema(job.getCp(), params);
		File inputFile = new File(job.getInputDir(), params.inputFilename);
		if (!inputFile.exists()) {
			return;
		}

		String inputFilePath = inputFile.getAbsolutePath();
		String outputFilePath = new File(job.getOutputDir(), params.inputFilename).getAbsolutePath();

		ObjectReader reader = new ObjectReader(inputFilePath, schema, DATE_FMT, DATE_TIME_FMT, FIELD_SEPARATOR);
		reader.setTimeZone("UTC");

		MobileUploadJob.Status status = job.getStatus();
		long totalRecords = job.getTotalRecords(), failedRecords = job.getFailedRecords();
		CsvWriter writer = null;
		try {
			writer = getWriter(outputFilePath);
			List<String> headerRow = reader.getCsvColumnNames();
			headerRow.addAll(Arrays.asList("OS_IMPORT_STATUS", "OS_ERROR_MESSAGE"));
			writer.writeNext(headerRow.toArray(new String[0]));

			while (true) {
				Map<String, Object> record = (Map<String, Object>) reader.next();
				if (record == null) {
					break;
				}

				record.putAll((Map<String, Object>) record.remove("formValueMap"));
				++totalRecords;

				String errMsg = null;
				try {
					importer.apply(groupFields(record));
				} catch (Throwable t) {
					errMsg = t.getMessage();
					if (StringUtils.isBlank(errMsg)) {
						errMsg = "Internal Server Error";
					}
				} finally {
					clearSession();
				}

				List<String> row = reader.getCsvRow();
				if (errMsg != null) {
					row.addAll(Arrays.asList("FAIL", errMsg));
					++failedRecords;
				} else {
					row.addAll(Arrays.asList("SUCCESS", ""));
				}

				writer.writeNext(row.toArray(new String[0]));
				if (totalRecords % 25 == 0) {
					writer.flush();
					saveJob(job, totalRecords, failedRecords);
				}
			}
		} catch (Throwable t) {
			String[] errorLine = null;
			if (t instanceof CsvException) {
				errorLine = ((CsvException) t).getErroneousLine();
			}

			if (errorLine == null) {
				errorLine = new String[] { t.getMessage() };
			}

			if (writer != null) {
				writer.writeNext(errorLine);
				writer.writeNext(new String[] { ExceptionUtils.getFullStackTrace(t) });
			}

			job.setStatus(MobileUploadJob.Status.FAILED);
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(writer);

			saveJob(job, totalRecords, failedRecords);
		}
	}

	private ObjectSchema getSchema(CollectionProtocol cp, SchemaParams params) {
		List<ObjectSchema.Field> fields = new ArrayList<>();
		switch (params.action) {
			case "registerParticipant":
				fields.add(getField("cpShortTitle", "Collection Protocol"));
				break;

			case "addSpecimen":
				fields.add(getField("cpShortTitle", "Collection Protocol"));
				fields.add(getField("ppid", "PPID"));
				break;

			case "createAliquots":
				fields.add(getField("cpShortTitle", "Collection Protocol"));
				fields.add(getField("ppid", "PPID"));
				fields.add(getField("parentLabel", "Parent Label"));
				break;
		}

		ObjectSchema.Record record = new ObjectSchema.Record();
		record.setFields(fields);

		Container form = getForm(cp.getId(), params.entity, params.viewForm);
		FormSchemaBuilder builder = new FormSchemaBuilder();
		ObjectSchema.Record formValueMap = builder.getFormRecord(form);
		formValueMap.setAttribute("formValueMap");
		record.setSubRecords(Collections.singletonList(formValueMap));

		ObjectSchema objectSchema = new ObjectSchema();
		objectSchema.setRecord(record);
		return objectSchema;
	}

	private ObjectSchema.Field getField(String attr, String caption) {
		ObjectSchema.Field field = new ObjectSchema.Field();
		field.setAttribute(attr);
		field.setCaption(caption);
		return field;
	}

	private void clearSession() {
		try {
			sessionFactory.getCurrentSession().flush();
		} catch (Exception e) {
			//
			// Oops, we encountered error. This happens when we've received database errors
			// like data truncation error, unique constraint etc ... We can't do much except
			// log and move forward
			//
			logger.info("Error flushing the database session", e);
		} finally {
			try {
				sessionFactory.getCurrentSession().clear();
			} catch (Exception e) {
				//
				// Something severely wrong...
				//
				logger.error("Error cleaning the database session", e);
			}
		}
	}

	private CsvWriter getWriter(String outputFilePath) {
		try {
			return CsvFileWriter.createCsvFileWriter(new FileWriter(outputFilePath), ',', '"');
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	private void sendJobStatusNotification(MobileUploadJob job) {
		try {
			String [] subjParams = new String[] {
				job.getId().toString(),
				job.getCp().getShortTitle()
			};

			String statusKey = "bulk_import_statuses_" + job.getStatus();
			Map<String, Object> props = new HashMap<>();
			props.put("job", job);
			props.put("cp", job.getCp());
			props.put("status", MessageUtil.getInstance().getMessage(statusKey));
			props.put("$subject", subjParams);
			props.put("ccAdmin", true);

			String[] rcpts = {job.getCreatedBy().getEmailAddress()};
			EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
		} catch (Throwable t) {
			logger.error("Failed to send import job status e-mail notification", t);
		}
	}

	private void tidyFiles(MobileUploadJob job) {
		try {
			FileUtils.deleteDirectory(job.getInputDir());
			IoUtil.zipFiles(
				job.getOutputDir().getAbsolutePath(),
				new File(job.getWorkingDir(), "output.zip").getAbsolutePath(),
				Collections.emptyList());
			FileUtils.deleteDirectory(job.getOutputDir());
		} catch (Exception e) {
			logger.error("Error tidying the files", e);
		}
	}

	private static class FormSchemaBuilder extends ExtensionSchemaBuilder {
		protected ObjectSchema.Record getFormRecord(Container form) {
			return getFormRecord(form, true);
		}
	}

	private static class SchemaParams {
		public String entity;

		public String viewForm;

		public String action;

		public String inputFilename;


		public SchemaParams(String entity, String viewForm, String action, String inputFilename) {
			this.entity = entity;
			this.viewForm = viewForm;
			this.action = action;
			this.inputFilename = inputFilename;
		}
	}

	private static final String FLUID_NS = "Fluid - Not Specified";

	private static final String NS = "Not Specified";

	private static final String DATE_FMT = "yyyy-MM-dd";

	private static final String DATE_TIME_FMT = "yyyy-MM-dd'T'HH:mm'Z'";

	private static final String FIELD_SEPARATOR = ",";

	private static final String JOB_STATUS_EMAIL_TMPL = "mobile_upload_status_notif";

	private static final String GET_RECORDS_SQL =
		"select" +
		"  re.record_id, user.identifier as user_id, user.first_name, user.last_name, " +
		"  re.update_time, fc.identifier as form_ctxt_id, fc.entity_type, " +
		"  f.identifier as form_id, f.caption " +
		"from " +
		"  catissue_form_record_entry re " +
		"  inner join catissue_form_context fc on fc.identifier = re.form_ctxt_id " +
		"  inner join dyextn_containers f on f.identifier = fc.container_id " +
		"  inner join catissue_user user on user.identifier = re.updated_by " +
		"where " +
		"  re.activity_status = 'ACTIVE' and " +
		"  fc.deleted_on is null and " +
		"  f.deleted_on is null and " +
		"  re.object_id = :objectId and " +
		"  fc.entity_type = :entityType and " +
		"  f.name in (:formNames) " +
		"order by " +
		"  re.update_time desc";
}
