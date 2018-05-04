package com.krishagni.catissueplus.core.importer.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.events.ImportDetail;
import com.krishagni.catissueplus.core.importer.events.ImportJobDetail;

@Configurable
public class ImportRecordsTask implements ScheduledTask {
	private static Log logger = LogFactory.getLog(ImportRecordsTask.class);

	private static final String TSTAMP_FMT = "yyyyMMddHHmmssSSS";

	private static File scheduledImportDir;

	private static File processedImportDir;

	private static File unprocessedImportDir;

	@Autowired
	private ImportServiceImpl importService;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		logger.info("Woken up to bulk import records");
		if (!getScheduledImportDir().exists()) {
			logger.info("Scheduled import directory does not exist. Therefore sleeping until next time");
			return;
		}

		List<File> filesToImport = getImportFilesList();
		setCurrentUser();
		importFiles(filesToImport);
	}

	private List<File> getImportFilesList() {
		logger.info("Initialising list of files to bulk import");

		SimpleDateFormat sdf = new SimpleDateFormat(TSTAMP_FMT);
		File[] files = getScheduledImportDir().listFiles();
		if (files == null) {
			return Collections.emptyList();
		}

		TreeMap<Date, File> filesMap = new TreeMap<>();
		for (File file : files) {
			//
			// File name format should be
			// For OS entities: <object_type>_<operation>_<timestamp>_[<csv_type>].csv
			// Examples:  1) cpr_create_201605111162033124.csv
			//            2) distributionOrder_create_201605111162033124_m.csv
			//
			// For DE forms: extensions_<object_type>_<form_name>_<operation>_<timestamp>.csv
			// Example - extensions_Participant_familyHistoryAnnotation_create_20160511162246252.csv
			//
			String filename = file.getName();
			String[] tokens = filename.split("_");

			if (tokens.length < 3 || tokens.length > 5) {
				logger.info(String.format("Filename '%s' is not in correct format", filename));
				moveFileToUnprocessedDir(file);
				continue;
			}

			String timestampStr = tokens.length == 5 ? tokens[4] : tokens[2];
			Date timestamp = null;
			try {
				timestamp = sdf.parse(timestampStr);
			} catch (ParseException e) {
				logger.error("Appended timestamp in filename is not in correct format: " + timestampStr, e);
				moveFileToUnprocessedDir(file);
				continue;
			}

			filesMap.put(timestamp, file);
		}

		logger.info(String.format("Found %d files to import", filesMap.size()));
		return new ArrayList<>(filesMap.values());
	}

	@PlusTransactional
	private void setCurrentUser() {
		User user = daoFactory.getUserDao().getSystemUser();
		AuthUtil.setCurrentUser(user);
	}

	private void importFiles(List<File> files) {
		files.forEach(this::importFile);
	}

	private void importFile(File file) {
		logger.info("Starting to import records from file: " + file.getName());

		FileInputStream in = null;
		boolean processed = false;
		try {
			in = FileUtils.openInputStream(file);

			String fileId = importService.uploadImportJobFile(new RequestEvent<>(in)).getPayload();
			ImportDetail detail = getImportDetail(file, fileId);
			ResponseEvent<ImportJobDetail> resp = importService.importObjects(new RequestEvent<>(detail));
			resp.throwErrorIfUnsuccessful();
			processed = true;

			logger.info(String.format("Import job %d created to import records from file: %s", resp.getPayload().getId(), file.getName()));
		} catch (Exception e) {
			logger.error("Error importing records from file: " + file.getName(), e);
		} finally {
			IOUtils.closeQuietly(in);
			if (processed) {
				moveFileToProcessedDir(file);
			} else {
				moveFileToUnprocessedDir(file);
			}
		}
	}

	private ImportDetail getImportDetail(File file, String fileId) {
		String filename = FilenameUtils.getBaseName(file.getName());
		String[] tokens = filename.split("_");

		int i = 0;
		ImportDetail detail = new ImportDetail();
		detail.setInputFileId(fileId);
		detail.setObjectType(tokens[i++]);

		//
		// For DE forms set entity type and form name
		//
		if (tokens.length == 5) {
			Map<String, String> objParams = new HashMap<>();
			objParams.put("entityType", tokens[i++]);
			objParams.put("formName", tokens[i++]);
			detail.setObjectParams(objParams);
		}

		detail.setImportType(tokens[i++].toUpperCase());

		//
		// Set csv type
		//
		ImportJob.CsvType csvType = ImportJob.CsvType.SINGLE_ROW_PER_OBJ;
		if (tokens.length == 4 && tokens[3].trim().equalsIgnoreCase("m")) {
			csvType = ImportJob.CsvType.MULTIPLE_ROWS_PER_OBJ;
		}
		detail.setCsvType(csvType.toString());

		return detail;
	}

	private void moveFileToProcessedDir(File file) {
		move(file, getProcessedImportDir());
	}

	private void moveFileToUnprocessedDir(File file) {
		move(file, getUnprocessedImportDir());
	}

	private void move(File file, File destDir) {
		try {
			FileUtils.moveFileToDirectory(file, destDir, true);
		} catch (Exception e) {
			//
			// TODO: this might create an infinite loop.
			//
			logger.error(String.format("Error moving file %s to directory %s", file.getName(), destDir.getPath()), e);
		}
	}

	private File getScheduledImportDir() {
		if (scheduledImportDir == null) {
			scheduledImportDir = new File(getDataDir() + File.separator + "scheduled-bulk-import");
		}

		return scheduledImportDir;
	}

	private File getProcessedImportDir() {
		if (processedImportDir == null) {
			processedImportDir = new File(getDataDir() + File.separator + "processed-bulk-import");
		}

		return processedImportDir;
	}

	private File getUnprocessedImportDir() {
		if (unprocessedImportDir == null) {
			unprocessedImportDir = new File(getDataDir() + File.separator + "unprocessed-bulk-import");
		}

		return unprocessedImportDir;
	}

	private String getDataDir() {
		return ConfigUtil.getInstance().getDataDir();
	}
}
