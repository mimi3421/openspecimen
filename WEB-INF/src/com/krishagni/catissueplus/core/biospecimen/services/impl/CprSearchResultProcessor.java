package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.events.SearchResult;
import com.krishagni.catissueplus.core.common.service.SearchResultProcessor;

public class CprSearchResultProcessor implements SearchResultProcessor {
	private static final List<String> PHI_PROPS = Arrays.asList("uid", "empi", "medicalRecordNumber");

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public String getEntity() {
		return CollectionProtocolRegistration.getEntityName();
	}

	@Override
	public List<SearchResult> process(List<SearchResult> matches) {
		Map<Long, SearchResult> matchesMap = new LinkedHashMap<>();
		for (SearchResult match : matches) {
			if (matchesMap.containsKey(match.getEntityId())) {
				continue;
			}

			matchesMap.put(match.getEntityId(), match);
		}

		AccessCtrlMgr.ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess();
		if (!access.admin) {
			if (access.noAccessibleSites()) {
				return Collections.emptyList();
			}

			if (!access.phiAccess) {
				matchesMap.entrySet().removeIf(e -> PHI_PROPS.contains(e.getValue().getKey()));
			}
		}

		if (matchesMap.isEmpty()) {
			return Collections.emptyList();
		}


		CprListCriteria crit = new CprListCriteria()
			.ids(new ArrayList<>(matchesMap.keySet()))
			.siteCps(access.siteCps)
			.phiSiteCps(access.phiSiteCps)
			.includePhi(access.phiAccess)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

		List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprs(crit);
		for (CollectionProtocolRegistration cpr : cprs) {
			Map<String, Object> props = new HashMap<>();
			props.put("ppid", cpr.getPpid());
			props.put("cp", cpr.getCollectionProtocol().getShortTitle());
			matchesMap.get(cpr.getId()).setEntityProps(props);

			if (!access.phiAccess) {
				continue;
			}

			Participant participant = cpr.getParticipant();
			if (StringUtils.isNotBlank(participant.getEmpi())) {
				props.put("empi", participant.getEmpi());
			}

			if (StringUtils.isNotBlank(participant.getUid())) {
				props.put("uid", participant.getUid());
			}

			String mrns = participant.getPmis().stream()
				.map(pmi -> pmi.getMedicalRecordNumber())
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.joining(","));
			if (StringUtils.isNotBlank(mrns)) {
				props.put("mrns", mrns);
			}
		}

		matchesMap.entrySet().removeIf(e -> e.getValue().getEntityProps().isEmpty());
		return new ArrayList<>(matchesMap.values());
	}
}
