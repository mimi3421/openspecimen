
package com.krishagni.catissueplus.core.notification.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.VelocityClassLoaderManager;
import com.krishagni.catissueplus.core.common.email.EmailClient;
import com.krishagni.catissueplus.core.notification.domain.ExtAppNotificationStatus;
import com.krishagni.catissueplus.core.notification.schedular.ExternalAppNotificationSchedular;
import com.krishagni.catissueplus.core.notification.services.EmailNotificationService;

import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.logger.Logger;

public class EmailNotificationServiceImpl implements EmailNotificationService {

	private static final Logger LOGGER = Logger.getCommonLogger(ExternalAppNotificationSchedular.class);

	private DaoFactory daoFactory;

	private static String FAILED_NOTIFICATION_EMAIL_TMPL = "failed.notificationTemplate";

	private static final String KEY_EMAIL_ADMIN_EMAIL_ADDRESS = "email.administrative.emailAddress";

	private static final String adminEmailAddress = XMLPropertyHandler.getValue(KEY_EMAIL_ADMIN_EMAIL_ADDRESS);

	private static final String ERROR_WHILE_SENDING_MAIL = "Error while sending failed notifications mail";

	private static final String FAILED_NOTIFICATION_CSV_TMPL_FILE_PATH = "../resources/ng-file-templates/failed.notificationCsvTemplate.vm";

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public void sendFailedNotificationReport() {
		try {
			List<ExtAppNotificationStatus> expiredNotifications = daoFactory.getExternalAppNotificationDao()
					.getExpiredNotificationObjects();
			if (expiredNotifications != null && !expiredNotifications.isEmpty()) {
				Map<String, Object> failedNotificationDetails = new HashMap<String, Object>();
				failedNotificationDetails.put("failedNotifications", expiredNotifications);
				String filedata = null;
				filedata = VelocityClassLoaderManager.getInstance().evaluate(failedNotificationDetails,
						FAILED_NOTIFICATION_CSV_TMPL_FILE_PATH);
				DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy"); 
				String currentDate = dateFormat.format(new Date());
				String filename = "FailedNotifications_" + currentDate + ".csv";
				File file = new File(filename);
				file.createNewFile();
				FileOutputStream out = new FileOutputStream(file);
				out.write(filedata.getBytes());
				out.close();
				List<File> attachements = new ArrayList<File>();
				attachements.add(file);
				Map<String, Object> contextMap = new HashMap<String, Object>();
				EmailClient.getInstance().sendEmailWithAttachment(FAILED_NOTIFICATION_EMAIL_TMPL,
						new String[]{adminEmailAddress}, null, null, attachements, contextMap, null);
				file.delete();
			}

		}
		catch (Exception ex) {
			LOGGER.error(ERROR_WHILE_SENDING_MAIL, ex);
		}
	}
}