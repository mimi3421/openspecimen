package com.krishagni.catissueplus.core.importer.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.MergedObject;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvException;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.CsvType;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.Status;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.Type;
import com.krishagni.catissueplus.core.importer.domain.ImportJobErrorCode;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.events.FileRecordsDetail;
import com.krishagni.catissueplus.core.importer.events.ImportDetail;
import com.krishagni.catissueplus.core.importer.events.ImportJobDetail;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.events.ObjectSchemaCriteria;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;
import com.krishagni.catissueplus.core.importer.services.ObjectImporterFactory;
import com.krishagni.catissueplus.core.importer.services.ObjectReader;
import com.krishagni.catissueplus.core.importer.services.ObjectSchemaFactory;

import edu.common.dynamicextensions.query.cachestore.LinkedEhCacheMap;
import edu.common.dynamicextensions.util.ZipUtility;

public class ImportServiceImpl implements ImportService, ApplicationListener<ContextRefreshedEvent> {
	private static final Log logger = LogFactory.getLog(ImportServiceImpl.class);

	private static final int MAX_RECS_PER_TXN = 5000;

	private static final String CFG_MAX_TXN_SIZE = "import_max_records_per_txn";

	private ConfigurationService cfgSvc;

	private ImportJobDao importJobDao;

	private ObjectSchemaFactory schemaFactory;
	
	private ObjectImporterFactory importerFactory;
	
	private TransactionTemplate txTmpl;

	private TransactionTemplate newTxTmpl;

	private BlockingQueue<ImportJob> queuedJobs = new LinkedBlockingQueue<>();

	private volatile ImportJob currentJob = null;

	private volatile long lastRefreshTime = 0;

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setImportJobDao(ImportJobDao importJobDao) {
		this.importJobDao = importJobDao;
	}
	
	public void setSchemaFactory(ObjectSchemaFactory schemaFactory) {
		this.schemaFactory = schemaFactory;
	}

	public void setImporterFactory(ObjectImporterFactory importerFactory) {
		this.importerFactory = importerFactory;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.txTmpl = new TransactionTemplate(transactionManager);
		this.txTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		this.newTxTmpl = new TransactionTemplate(transactionManager);
		this.newTxTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ImportJobDetail>> getImportJobs(RequestEvent<ListImportJobsCriteria> req) {
		try {
			ListImportJobsCriteria crit = req.getPayload();
			if (AuthUtil.isInstituteAdmin()) {
				crit.instituteId(AuthUtil.getCurrentUserInstitute().getId());
			} else if (!AuthUtil.isAdmin()) {
				crit.userId(AuthUtil.getCurrentUser().getId());
			}
			
			List<ImportJob> jobs = importJobDao.getImportJobs(crit);			
			return ResponseEvent.response(ImportJobDetail.from(jobs));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ImportJobDetail> getImportJob(RequestEvent<Long> req) {
		try {
			ImportJob job = getImportJob(req.getPayload());
			return ResponseEvent.response(ImportJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<String> getImportJobFile(RequestEvent<Long> req) {
		try {
			ImportJob job = getImportJob(req.getPayload());
			File file = new File(getJobOutputFilePath(job.getId()));
			if (!file.exists()) {
				return ResponseEvent.userError(ImportJobErrorCode.OUTPUT_FILE_NOT_CREATED);
			}
			
			return ResponseEvent.response(file.getAbsolutePath());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	public ResponseEvent<String> uploadImportJobFile(RequestEvent<InputStream> req) {
		OutputStream out = null;
		
		try {
			//
			// 1. Ensure import directory is present
			//
			String importDir = getImportDir();			
			new File(importDir).mkdirs();
			
			//
			// 2. Generate unique file ID
			//
			String fileId = UUID.randomUUID().toString();
			
			//
			// 3. Copy uploaded file to import directory
			//
			InputStream in = req.getPayload();			
			out = new FileOutputStream(importDir + File.separator + fileId);
			IOUtils.copy(in, out);
			
			return ResponseEvent.response(fileId);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}	
	
	@Override
	public ResponseEvent<ImportJobDetail> importObjects(RequestEvent<ImportDetail> req) {
		ImportJob job = null;
		try {
			ImportDetail detail = req.getPayload();
			String inputFile = getFilePath(detail.getInputFileId());

			if (isZipFile(inputFile)) {
				inputFile = inflateAndGetInputCsvFile(inputFile, detail.getInputFileId());
			}

			//
			// Ensure transaction size is well within configured limits
			//
			int inputRecordsCnt = CsvFileReader.getRowsCount(inputFile, true);
			if (detail.isAtomic() && inputRecordsCnt > getMaxRecsPerTxn()) {
				return ResponseEvent.response(ImportJobDetail.txnSizeExceeded(inputRecordsCnt));
			}

			job = createImportJob(detail);

			//
			// Set up file in job's directory
			//
			createJobDir(job.getId());
			moveToJobDir(inputFile, job.getId());

			queuedJobs.offer(job);
			return ResponseEvent.response(ImportJobDetail.from(job));
		} catch (CsvException csve) {
			return ResponseEvent.userError(ImportJobErrorCode.RECORD_PARSE_ERROR, csve.getLocalizedMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<ImportJobDetail> stopJob(RequestEvent<Long> req) {
		try {
			Long jobId = req.getPayload();

			ImportJob toRemove = null;
			for (ImportJob job : queuedJobs) {
				if (job.getId().equals(jobId)) {
					toRemove = job;
					break;
				}
			}

			if (toRemove == null && currentJob != null && currentJob.getId().equals(jobId)) {
				toRemove = currentJob;
			}

			if (toRemove == null || (!toRemove.isQueued() && !toRemove.isInProgress())) {
				return ResponseEvent.userError(ImportJobErrorCode.NOT_IN_PROGRESS, req.getPayload());
			}

			ensureAccess(toRemove);
			toRemove.stop();
			queuedJobs.remove(toRemove);

			for (int i = 0; i < 5; ++i) {
				TimeUnit.SECONDS.sleep(1);
				if (!toRemove.isQueued() && !toRemove.isInProgress()) {
					break;
				}
			}

			return ResponseEvent.response(ImportJobDetail.from(toRemove));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<String> getInputFileTemplate(RequestEvent<ObjectSchemaCriteria> req) {
		try {
			ObjectSchemaCriteria detail = req.getPayload();
			ObjectSchema schema = schemaFactory.getSchema(detail.getObjectType(), detail.getParams());
			if (schema == null) {
				return ResponseEvent.userError(ImportJobErrorCode.OBJ_SCHEMA_NOT_FOUND, detail.getObjectType());
			}

			return ResponseEvent.response(ObjectReader.getSchemaFields(schema, detail.getFieldSeparator()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<List<Map<String, Object>>> processFileRecords(RequestEvent<FileRecordsDetail> req) {
		ObjectReader reader = null;
		File file = null;

		try {
			FileRecordsDetail detail = req.getPayload();

			ObjectSchema.Record schemaRec = new ObjectSchema.Record();
			schemaRec.setFields(detail.getFields());

			ObjectSchema schema = new ObjectSchema();
			schema.setRecord(schemaRec);

			file = new File(getFilePath(detail.getFileId()));
			reader = new ObjectReader(
				file.getAbsolutePath(),
				schema,
				ConfigUtil.getInstance().getDeDateFmt(),
				ConfigUtil.getInstance().getTimeFmt());

			List<Map<String, Object>> records = new ArrayList<>();
			Map<String, Object> record = null;
			while ((record = (Map<String, Object>)reader.next()) != null) {
				records.add(record);
			}

			return ResponseEvent.response(records);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(reader);
			if (file != null) {
				file.delete();
			}
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		boolean startScheduler = (lastRefreshTime == 0L);
		lastRefreshTime = System.currentTimeMillis();
		if (!startScheduler) {
			return;
		}

		Executors.newSingleThreadExecutor().submit(() -> {
			try {
				while ((System.currentTimeMillis() - lastRefreshTime) < 60 * 1000) {
					Thread.sleep(10 * 1000);
				}

				logger.info("Starting bulk import jobs scheduler");
				runImportJobScheduler();
			} catch (Exception e) {
				logger.fatal("Bulk import jobs scheduler thread got killed", e);
			}
		});
	}

	private boolean isZipFile(String zipFilename) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zipFilename);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			IOUtils.closeQuietly(zipFile);
		}
	}

	private String inflateAndGetInputCsvFile(String zipFile, String fileId) {
		FileInputStream in = null;
		String inputFile = null;
		try {
			in = new FileInputStream(zipFile);

			File zipDirFile = new File(getImportDir() + File.separator + "inflated-" + fileId);
			ZipUtility.extractZipToDestination(in, zipDirFile.getAbsolutePath());

			inputFile = Arrays.stream(zipDirFile.listFiles())
				.filter(f -> !f.isDirectory() && f.getName().endsWith(".csv"))
				.map(File::getAbsolutePath)
				.findFirst()
				.orElse(null);
		
			if (inputFile == null) {
				throw OpenSpecimenException.userError(ImportJobErrorCode.CSV_NOT_FOUND_IN_ZIP, zipFile);
			}
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(in);
		}

		return inputFile;
	}

	private int getMaxRecsPerTxn() {
		return ConfigUtil.getInstance().getIntSetting("common", CFG_MAX_TXN_SIZE, MAX_RECS_PER_TXN);
	}

	private ImportJob getImportJob(Long jobId) {
		ImportJob job = importJobDao.getById(jobId);
		if (job == null) {
			throw OpenSpecimenException.userError(ImportJobErrorCode.NOT_FOUND, jobId);
		}

		return ensureAccess(job);
	}

	private ImportJob ensureAccess(ImportJob job) {
		User currentUser = AuthUtil.getCurrentUser();
		if (!currentUser.isAdmin() && !currentUser.equals(job.getCreatedBy())) {
			throw OpenSpecimenException.userError(ImportJobErrorCode.ACCESS_DENIED);
		}

		return job;
	}
	
	private String getDataDir() {
		return cfgSvc.getDataDir();
	}
	
	private String getImportDir() {
		return getDataDir() + File.separator + "bulk-import";
	}
	
	private String getFilePath(String fileId) { 
		 return getImportDir() + File.separator + fileId;
	}
	
	private String getJobsDir() {
		return getDataDir() + File.separator + "bulk-import" + File.separator + "jobs";
	}
	
	private String getJobDir(Long jobId) {
		return getJobsDir() + File.separator + jobId;
	}
	
	private String getJobOutputFilePath(Long jobId) {
		return getJobDir(jobId) + File.separator + "output.csv";
	}
	
	private String getFilesDirPath(Long jobId) {
		return getJobDir(jobId) + File.separator + "files";
	}
	
	private boolean createJobDir(Long jobId) {
		return new File(getJobDir(jobId)).mkdirs();
	}
	
	private void moveToJobDir(String file, Long jobId) {
		String jobDir = getJobDir(jobId);

		//
		// Input file and its parent directory
		//
		File inputFile = new File(file);
		File srcDir = inputFile.getParentFile();

		//
		// Move input CSV file to job dir
		//
		inputFile.renameTo(new File(jobDir + File.separator + "input.csv"));

		//
		// Move other uploaded files to job dir as well
		//
		File filesDir = new File(srcDir + File.separator + "files");
		if (filesDir.exists()) {
			filesDir.renameTo(new File(getFilesDirPath(jobId)));
		}
	}

	@PlusTransactional
	private ImportJob createImportJob(ImportDetail detail) { // TODO: ensure checks are done
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		ImportJob job = new ImportJob();
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setName(detail.getObjectType());
		job.setStatus(Status.QUEUED);
		job.setAtomic(detail.isAtomic());
		job.setParams(detail.getObjectParams());
		job.setFieldSeparator(detail.getFieldSeparator());

		setImportType(detail, job, ose);
		setCsvType(detail, job, ose);
		setDateAndTimeFormat(detail, job, ose);
		ose.checkAndThrow();

		importJobDao.saveOrUpdate(job, true);
		return job;		
	}

	private void setImportType(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String importType = detail.getImportType();
		job.setType(StringUtils.isBlank(importType) ? Type.CREATE : Type.valueOf(importType));
	}

	private void setCsvType(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String csvType = detail.getCsvType();
		job.setCsvtype(StringUtils.isBlank(csvType) ? CsvType.SINGLE_ROW_PER_OBJ : CsvType.valueOf(csvType));
	}

	private void setDateAndTimeFormat(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String dateFormat = detail.getDateFormat();
		if (StringUtils.isBlank(dateFormat)) {
			dateFormat = ConfigUtil.getInstance().getDeDateFmt();
		} else if (!Utility.isValidDateFormat(dateFormat)) {
			ose.addError(ImportJobErrorCode.INVALID_DATE_FORMAT, dateFormat);
			return;
		}

		job.setDateFormat(dateFormat);

		String timeFormat = detail.getTimeFormat();
		if (StringUtils.isBlank(timeFormat)) {
			timeFormat = ConfigUtil.getInstance().getTimeFmt();
		} else if (!Utility.isValidDateFormat(dateFormat + " " + timeFormat)) {
			ose.addError(ImportJobErrorCode.INVALID_TIME_FORMAT, timeFormat);
			return;
		}

		job.setTimeFormat(timeFormat);
	}

	private void runImportJobScheduler() {
		try {
			int failedJobs = markInProgressJobsAsFailed();
			logger.info("Marked " + failedJobs + " in-progress jobs from previous incarnation as failed");

			loadJobQueue();

			while (true) {
				logger.info("Probing for queued bulk import jobs");
				ImportJob job = queuedJobs.take();

				try {
					logger.info("Picked import job " + job.getId() + " for processing");
					new ImporterTask(job).run();
				} catch (Exception e) {
					logger.error("Error running the job " + job.getId() + " to completion", e);
				}
			}
		} catch (Exception e) {
			logger.error("Import jobs scheduler encountered a fatal exception. Stopping to run import jobs.", e);
		}
	}

	@PlusTransactional
	private int markInProgressJobsAsFailed() {
		return importJobDao.markInProgressJobsAsFailed();
	}

	@PlusTransactional
	private void loadJobQueue() {
		logger.info("Loading bulk import jobs queue");

		ListImportJobsCriteria crit = new ListImportJobsCriteria()
			.status(Status.QUEUED.name())
			.maxResults(100);

		int startAt = 0;
		boolean endOfJobs = false;
		while (!endOfJobs) {
			List<ImportJob> jobs = importJobDao.getImportJobs(crit.startAt(startAt));
			jobs.forEach(job -> {
				if (!Hibernate.isInitialized(job.getCreatedBy())) {
					Hibernate.initialize(job.getCreatedBy());
				}

				job.getParams().size();

				queuedJobs.offer(job);
			});

			endOfJobs = (queuedJobs.size() < 100);
			startAt += queuedJobs.size();
		}

		logger.info("Loaded " + startAt + " bulk import jobs in queue from previous incarnation");
	}

	private class ImporterTask implements Runnable {
		private ImportJob job;

		private boolean atomic;

		private long totalRecords = 0;

		private long failedRecords = 0;

		ImporterTask(ImportJob job) {
			this.job = job;
			this.atomic = Boolean.TRUE.equals(job.getAtomic());
		}

		@Override
		public void run() {
			ObjectReader objReader = null;
			CsvWriter csvWriter = null;

			try {
				currentJob = job;

				AuthUtil.setCurrentUser(job.getCreatedBy());
				ImporterContextHolder.getInstance().newContext();
				saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);

				ObjectSchema schema = schemaFactory.getSchema(job.getName(), job.getParams());
				String filePath = getJobDir(job.getId()) + File.separator + "input.csv";
				csvWriter = getOutputCsvWriter(schema, job);
				objReader = new ObjectReader(filePath, schema, job.getDateFormat(), job.getTimeFormat(), job.getFieldSeparator());

				List<String> columnNames = objReader.getCsvColumnNames();
				columnNames.add("OS_IMPORT_STATUS");
				columnNames.add("OS_ERROR_MESSAGE");

				csvWriter.writeNext(columnNames.toArray(new String[0]));

				Status status = processRows(objReader, csvWriter);
				saveJob(totalRecords, failedRecords, status);
			} catch (Exception e) {
				logger.error("Error running import records job", e);
				saveJob(totalRecords, failedRecords, Status.FAILED);

				String[] errorLine = null;
				if (e instanceof CsvException) {
					errorLine = ((CsvException) e).getErroneousLine();
				}

				if (errorLine == null) {
					errorLine = new String[] { e.getMessage() };
				}

				if (csvWriter != null) {
					csvWriter.writeNext(errorLine);
					csvWriter.writeNext(new String[] { ExceptionUtils.getFullStackTrace(e) });
				}
			} finally {
				ImporterContextHolder.getInstance().clearContext();
				AuthUtil.clearCurrentUser();
				currentJob = null;

				IOUtils.closeQuietly(objReader);
				closeQuietly(csvWriter);

				//
				// Delete uploaded files
				//
				FileUtils.deleteQuietly(new File(getFilesDirPath(job.getId())));
				sendJobStatusNotification();
			}
		}

		private Status processRows(ObjectReader objReader, CsvWriter csvWriter) {
			if (atomic) {
				return txTmpl.execute(new TransactionCallback<Status>() {
					@Override
					public Status doInTransaction(TransactionStatus txnStatus) {
						Status jobStatus = processRows0(objReader, csvWriter);
						if (jobStatus == Status.FAILED || jobStatus == Status.STOPPED) {
							txnStatus.setRollbackOnly();
						}

						return jobStatus;
					}
				});
			} else {
				return processRows0(objReader, csvWriter);
			}
		}

		private Status processRows0(ObjectReader objReader, CsvWriter csvWriter) {
			boolean stopped;
			ObjectImporter<Object, Object> importer = importerFactory.getImporter(job.getName());
			if (job.getCsvtype() == CsvType.MULTIPLE_ROWS_PER_OBJ) {
				stopped = processMultipleRowsPerObj(objReader, csvWriter, importer);
			} else {
				stopped = processSingleRowPerObj(objReader, csvWriter, importer);
			}

			return stopped ? Status.STOPPED : (failedRecords > 0) ? Status.FAILED : Status.COMPLETED;
		}

		private boolean processSingleRowPerObj(ObjectReader objReader, CsvWriter csvWriter, ObjectImporter<Object, Object> importer) {
			boolean stopped = false;
			while (!job.isAskedToStop() || !(stopped = true)) {
				String errMsg = null;
				try {
					Object object = objReader.next();
					if (object == null) {
						break;
					}
	
					errMsg = importObject(importer, object, job.getParams());
				} catch (OpenSpecimenException ose) {
					errMsg = ose.getMessage();
				}
				
				++totalRecords;
				
				List<String> row = objReader.getCsvRow();
				if (StringUtils.isNotBlank(errMsg)) {
					row.add("FAIL");
					row.add(errMsg);
					++failedRecords;
				} else {
					row.add("SUCCESS");
					row.add("");
				}
				
				csvWriter.writeNext(row.toArray(new String[0]));
				if (totalRecords % 25 == 0) {
					saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);
				}
			}

			return stopped;
		}

		private boolean processMultipleRowsPerObj(ObjectReader objReader, CsvWriter csvWriter, ObjectImporter<Object, Object> importer) {
			LinkedEhCacheMap<String, MergedObject> objectsMap =  new LinkedEhCacheMap<>();
			
			while (true) {
				String errMsg = null;
				Object parsedObj = null;
				
				try {
					parsedObj = objReader.next();
					if (parsedObj == null) {
						break;
					}
				} catch (Exception e) {
					errMsg = e.getMessage();
					if (errMsg == null) {
						errMsg = e.getClass().getName();
					}
				}
				
				String key = objReader.getRowKey();
				MergedObject mergedObj = objectsMap.get(key);
				if (mergedObj == null) {
					mergedObj = new MergedObject();
					mergedObj.setKey(key);
					mergedObj.setObject(parsedObj);
				}

				if (errMsg != null) {
					//
					// mark the object as processed whenever an error is encountered.
					//
					mergedObj.addErrMsg(errMsg);
					mergedObj.setProcessed(true);
				}

				mergedObj.addRow(objReader.getCsvRow());
				mergedObj.merge(parsedObj);
				objectsMap.put(key, mergedObj);
			}

			boolean stopped = false;
			Iterator<MergedObject> mergedObjIter = objectsMap.iterator();
			while (mergedObjIter.hasNext() && (!job.isAskedToStop() || !(stopped = true))) {
				MergedObject mergedObj = mergedObjIter.next();
				if (!mergedObj.isErrorneous()) {
					String errMsg = null;
					try {
						errMsg = importObject(importer, mergedObj.getObject(), job.getParams());
					} catch (OpenSpecimenException ose) {
						errMsg = ose.getMessage();
					}

					if (StringUtils.isNotBlank(errMsg)) {
						mergedObj.addErrMsg(errMsg);
					}

					mergedObj.setProcessed(true);
					objectsMap.put(mergedObj.getKey(), mergedObj);
				}
				
				++totalRecords;
				if (mergedObj.isErrorneous()) {
					++failedRecords;
				}
				
				if (totalRecords % 25 == 0) {
					saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);
				}
			}
			
			mergedObjIter = objectsMap.iterator();
			while (mergedObjIter.hasNext()) {
				MergedObject mergedObj = mergedObjIter.next();
				csvWriter.writeAll(mergedObj.getRowsWithStatus());
			}

			objectsMap.clear();
			return stopped;
		}
		
		private String importObject(final ObjectImporter<Object, Object> importer, Object object, Map<String, String> params) {
			try {
				ImportObjectDetail<Object> detail = new ImportObjectDetail<>();
				detail.setCreate(job.getType() == Type.CREATE);
				detail.setObject(object);
				detail.setParams(params);
				detail.setUploadedFilesDir(getFilesDirPath(job.getId()));
				
				final RequestEvent<ImportObjectDetail<Object>> req = new RequestEvent<>(detail);
				ResponseEvent<Object> resp = txTmpl.execute(
						new TransactionCallback<ResponseEvent<Object>>() {
							@Override
							public ResponseEvent<Object> doInTransaction(TransactionStatus status) {
								ResponseEvent<Object> resp = importer.importObject(req);
								if (!resp.isSuccessful()) {
									status.setRollbackOnly();
								}
								
								return resp;
							}
						});

				if (atomic) {
					//
					// Let's give a clean session for every object to be imported
					//
					clearSession();
				}

				if (resp.isSuccessful()) {
					return null;
				} else {
					return resp.getError().getMessage();
				}
			} catch (Exception e) {
				if (StringUtils.isBlank(e.getMessage())) {
					return "Internal Server Error";
				} else {
					return e.getMessage();
				}
			}
		}
		
		private void saveJob(long totalRecords, long failedRecords, Status status) {
			job.setTotalRecords(totalRecords);
			job.setFailedRecords(failedRecords);
			job.setStatus(status);

			if (status != Status.IN_PROGRESS) {
				job.setEndTime(Calendar.getInstance().getTime());
			}

			newTxTmpl.execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					importJobDao.saveOrUpdate(job);
					return null;
				}
			});
		}

		private CsvWriter getOutputCsvWriter(ObjectSchema schema, ImportJob job)
		throws IOException {
			char fieldSeparator = Utility.getFieldSeparator();
			if (StringUtils.isNotBlank(job.getFieldSeparator())) {
				fieldSeparator = job.getFieldSeparator().charAt(0);
			} else if (StringUtils.isNotBlank(schema.getFieldSeparator())) {
				fieldSeparator = schema.getFieldSeparator().charAt(0);
			}

			FileWriter writer = new FileWriter(getJobOutputFilePath(job.getId()));
			return CsvFileWriter.createCsvFileWriter(writer, fieldSeparator, '"');
		}
				
		private void closeQuietly(CsvWriter writer) {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
											
				}
			}
		}

		private void sendJobStatusNotification() {
			try {
				String entityName = getEntityName();
				String op = getMsg("bulk_import_ops_" + job.getType());
				String [] subjParams = new String[] {
						job.getId().toString(),
						op,
						entityName
				};

				Map<String, Object> props = new HashMap<>();
				props.put("job", job);
				props.put("entityName", entityName);
				props.put("op", op);
				props.put("status", getMsg("bulk_import_statuses_" + job.getStatus()));
				props.put("atomic", atomic);
				props.put("$subject", subjParams);
				props.put("ccAdmin", isCopyNotifToAdminEnabled());

				String[] rcpts = {job.getCreatedBy().getEmailAddress()};
				EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
			} catch (Throwable t) {
				logger.error("Failed to send import job status e-mail notification", t);
			}
		}

		private String getEntityName() {
			String entityName;

			if (job.getName().equals(EXTENSIONS)) {
				entityName = job.getParams().get("formName") + " (" + job.getParams().get("entityType") + ")";
			} else {
				entityName = getMsg("bulk_import_entities_" + job.getName());
			}

			return entityName;
		}

		private void clearSession() {
			SessionUtil.getInstance().clearSession();
		}

		private boolean isCopyNotifToAdminEnabled() {
			return ConfigUtil.getInstance().getBoolSetting("notifications", "cc_import_emails_to_admin", false);
		}

		private String getMsg(String key, Object ... params) {
			return MessageUtil.getInstance().getMessage(key, params);
		}

		private static final String JOB_STATUS_EMAIL_TMPL = "import_job_status_notif";

		private static final String EXTENSIONS = "extensions";
	}
}
