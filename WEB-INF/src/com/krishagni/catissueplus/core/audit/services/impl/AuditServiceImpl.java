package com.krishagni.catissueplus.core.audit.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.domain.factory.AuditErrorCode;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditEntityQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionEntityRecordDetail;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.ObjectAccessorFactory;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class AuditServiceImpl implements AuditService {

	private static Log logger = LogFactory.getLog(AuditServiceImpl.class);

	private static final int ONLINE_EXPORT_TIMEOUT_SECS = 30;

	private static final String REV_EMAIL_TMPL = "audit_entity_revisions";

	private DaoFactory daoFactory;

	private ObjectAccessorFactory objectAccessorFactory;

	private ThreadPoolTaskExecutor taskExecutor;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setObjectAccessorFactory(ObjectAccessorFactory objectAccessorFactory) {
		this.objectAccessorFactory = objectAccessorFactory;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<AuditDetail>> getEntityAuditDetail(RequestEvent<List<AuditEntityQueryCriteria>> req) {
		return ResponseEvent.response(getEntityAuditDetail(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<RevisionDetail>> getEntityRevisions(RequestEvent<List<AuditEntityQueryCriteria>> req) {
		List<AuditEntityQueryCriteria> criteria = req.getPayload();
		ensureReadAccess(criteria);

		List<RevisionDetail> revisions = criteria.stream().map(this::getEntityRevisions)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		if (criteria.size() > 1) {
			Collections.sort(revisions, (r1, r2) -> r2.getChangedOn().compareTo(r1.getChangedOn()));
		}

		return ResponseEvent.response(revisions);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> exportRevisions(RequestEvent<RevisionsListCriteria> req) {
		if (!AuthUtil.isAdmin()) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

		RevisionsListCriteria criteria = req.getPayload();

		User user = null;
		if (criteria.userId() != null) {
			user = daoFactory.getUserDao().getById(criteria.userId());
			if (user == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND, criteria.userId());
			}
		}

		User currentUser     = AuthUtil.getCurrentUser();
		User revisionsByUser = user;

		File revisionsFile = null;
		Future<File> result = taskExecutor.submit(() -> exportRevisions(criteria, currentUser, revisionsByUser));
		try {
			revisionsFile = result.get(ONLINE_EXPORT_TIMEOUT_SECS, TimeUnit.SECONDS);
		} catch (TimeoutException te) {
			// timed out waiting for the response
		} catch (Exception ie) {
			throw OpenSpecimenException.serverError(ie);
		}

		return ResponseEvent.response(new ExportedFileDetail(getFileId(revisionsFile), revisionsFile));
	}

	@Override
	public ResponseEvent<File> getExportedRevisionsFile(RequestEvent<String> req) {
		String filename = req.getPayload() + "_" + AuthUtil.getCurrentUser().getId();
		return ResponseEvent.response(new File(getAuditDir(), filename));
	}

	@Override
	@PlusTransactional
	public void insertApiCallLog(UserApiCallLog userAuditLog) {
		daoFactory.getAuditDao().saveOrUpdate(userAuditLog);
	}

	@Override
	@PlusTransactional
	public long getTimeSinceLastApiCall(Long userId, String token) {
		Date lastApiCallTime = daoFactory.getAuditDao().getLatestApiCallTime(userId, token);
		long timeSinceLastApiCallInMilli = Calendar.getInstance().getTime().getTime() - lastApiCallTime.getTime();
		return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastApiCallInMilli);
	}

	private List<AuditDetail> getEntityAuditDetail(List<AuditEntityQueryCriteria> criteria) {
		ensureReadAccess(criteria);
		return criteria.stream().map(this::getEntityAuditDetail).collect(Collectors.toList());
	}

	private void ensureReadAccess(List<AuditEntityQueryCriteria> criteria) {
		for (AuditEntityQueryCriteria crit : criteria) {
			ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
			if (accessor == null) {
				throw OpenSpecimenException.userError(AuditErrorCode.ENTITY_NOT_FOUND, crit.getObjectName());
			}

			accessor.ensureReadAllowed(crit.getObjectId());
		}
	}

	private AuditDetail getEntityAuditDetail(AuditEntityQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getAuditDetail(accessor.getAuditTable(), crit.getObjectId());
	}

	private List<RevisionDetail> getEntityRevisions(AuditEntityQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getRevisions(accessor.getAuditTable(), crit.getObjectId());
	}

	@PlusTransactional
	private File exportRevisions(RevisionsListCriteria criteria, User exportedBy, User revisionsBy) {
		long startTime = System.currentTimeMillis();
		CsvFileWriter csvWriter = null;

		try {
			File outputFile = getOutputFile(exportedBy.getId());
			csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

			writeHeader(csvWriter);

			long lastRecId = 0, totalRecords = 0, lastChunk = 0;
			Map<String, String> context = new HashMap<>();
			while (true) {
				long t1 = System.currentTimeMillis();
				List<RevisionDetail> revisions = daoFactory.getAuditDao().getRevisions(criteria);
				System.err.println(criteria.lastId() + ", " + (System.currentTimeMillis() - t1) + " ms");

				if (revisions.isEmpty()) {
					break;
				}

				for (RevisionDetail revision : revisions) {
					totalRecords += writeToCsv(context, revision, csvWriter);
					lastRecId = revision.getLastRecordId();

					long currentChunk = totalRecords / 25;
					if (currentChunk != lastChunk) {
						csvWriter.flush();
						lastChunk = currentChunk;
					}
				}

				criteria.lastId(lastRecId);
			}

			csvWriter.flush();

			sendEmailNotif(criteria, exportedBy, revisionsBy, outputFile);
			return outputFile;
		} catch (Exception e) {
			logger.error("Error exporting audit revisions", e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
			logger.info("Audit revisions report generator finished in " +  (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	private void writeHeader(CsvWriter writer) {
		String[] keys = {
			"audit_rev_id", "audit_rev_tstmp", "audit_rev_user", "audit_rev_user_email",
			"audit_rev_entity_op", "audit_rev_entity_name", "audit_rev_entity_id"
		};

		writer.writeNext(Stream.of(keys).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));
	}

	//
	// Row format
	// revision number, rev date, user, rev type, entity name, entity id
	//
	private int writeToCsv(Map<String, String> context, RevisionDetail revision, CsvWriter writer)
	throws IOException {
		String revId     = revision.getRevisionId().toString();
		String dateTime  = Utility.getDateTimeString(revision.getChangedOn());

		String user      = null;
		String userEmail = null;
		if (revision.getChangedBy() != null) {
			user      = revision.getChangedBy().formattedName();
			userEmail = revision.getChangedBy().getEmailAddress();
		}

		Function<String, String> toMsg = (k) -> MessageUtil.getInstance().getMessage(k);
		int recsCount = 0;

		for (RevisionEntityRecordDetail record : revision.getRecords()) {
			String op = null;
			switch (record.getType()) {
				case 0:
					op = "audit_op_insert";
					break;

				case 1:
					op = "audit_op_update";
					break;

				case 2:
					op = "audit_op_delete";
					break;
			}

			String opDisplay  = context.computeIfAbsent(op, toMsg);
			String entityName = context.computeIfAbsent("audit_entity_" + record.getEntityName(), toMsg);
			String entityId   = record.getEntityId().toString();

			writer.writeNext(new String[] {revId, dateTime, user, userEmail, opDisplay, entityName, entityId});
			++recsCount;

			if (recsCount % 25 == 0) {
				writer.flush();
			}
		}

		return recsCount;
	}

	private File getOutputFile(Long userId) {
		File auditDir = getAuditDir();
		if (auditDir.exists()) {
			auditDir.mkdirs();
		}

		return new File(auditDir, UUID.randomUUID().toString() + "_" + userId);
	}

	private File getAuditDir() {
		return new File(ConfigUtil.getInstance().getDataDir(), "audit");
	}

	private void sendEmailNotif(RevisionsListCriteria criteria, User exportedBy, User revsBy, File revisionsFile) {
		Map<String, Object> emailProps = new HashMap<>();

		if (CollectionUtils.isNotEmpty(criteria.entityNames())) {
			String entities = criteria.entityNames().stream()
				.map(entityName -> MessageUtil.getInstance().getMessage("audit_entity_" + entityName))
				.collect(Collectors.joining(", "));
			emailProps.put("entities", entities);
		}

		emailProps.put("startDate", getDateTimeString(criteria.startDate()));
		emailProps.put("endDate",   getDateTimeString(criteria.endDate()));
		emailProps.put("user",      revsBy != null ? revsBy.formattedName() : null);
		emailProps.put("fileId",    getFileId(revisionsFile));
		emailProps.put("rcpt",      exportedBy.formattedName());

		EmailUtil.getInstance().sendEmail(
			REV_EMAIL_TMPL,
			new String[] { exportedBy.getEmailAddress() },
			null,
			emailProps
		);
	}

	private String getFileId(File revisionsFile) {
		if (revisionsFile == null) {
			return null;
		}

		return revisionsFile.getName().substring(0, revisionsFile.getName().lastIndexOf("_"));
	}

	private String getDateTimeString(Date dt) {
		return dt != null ? Utility.getDateTimeString(dt) : null;
	}
}