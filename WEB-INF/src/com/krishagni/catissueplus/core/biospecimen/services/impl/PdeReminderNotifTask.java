package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotifLink;
import com.krishagni.catissueplus.core.biospecimen.events.PdeTokenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.PdeTokenGenerator;
import com.krishagni.catissueplus.core.biospecimen.services.PdeTokenGeneratorRegistry;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class PdeReminderNotifTask implements ScheduledTask {

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private PdeTokenGeneratorRegistry pdeTokenGeneratorRegistry;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) {
		Long lastId = 0L;
		while (true) {
			List<PdeNotif> notifs = daoFactory.getPdeNotifDao().getPendingNotifs(lastId, 100);
			if (notifs.isEmpty()) {
				break;
			}

			lastId = sendReminders(notifs);
		}
	}

	private Long sendReminders(List<PdeNotif> notifs) {
		Long lastId = null;

		for (PdeNotif notif : notifs) {
			List<PdeTokenDetail> tokens = new ArrayList<>();
			for (PdeNotifLink link : notif.getLinks()) {
				if (!link.isPending()) {
					continue;
				}

				PdeTokenGenerator tokenGenerator = pdeTokenGeneratorRegistry.get(link.getType());
				if (tokenGenerator == null) {
					continue;
				}

				PdeTokenDetail token = tokenGenerator.getToken(notif.getCpr(), link.getTokenId());
				if (token == null) {
					continue;
				}

				tokens.add(token);
			}


			sendReminder(notif.getCpr(), tokens);
			lastId = notif.getId();
		}

		return lastId;
	}

	private void sendReminder(CollectionProtocolRegistration cpr, List<PdeTokenDetail> tokens) {
		if (tokens.isEmpty()) {
			return;
		}

		String name = cpr.getParticipant().formattedName();
		if (StringUtils.isBlank(name)) {
			name = cpr.getPpid();
		}

		Map<String, Object> props = new HashMap<>();
		props.put("participantName", name);
		props.put("cpr", cpr);
		props.put("tokens", tokens);
		props.put("cpShortTitle", cpr.getCollectionProtocol().getShortTitle());
		props.put("reminder", true);
		props.put("expiryTime", Utility.getDateTimeString(tokens.get(0).getExpiryTime()));
		props.put("$subject", new Object[] {2, cpr.getCollectionProtocol().getShortTitle(), cpr.getPpid() });

		EmailUtil.getInstance().sendEmail(
			"pde_links",
			new String[] { cpr.getParticipant().getEmailAddress() },
			null,
			props);
	}
}
