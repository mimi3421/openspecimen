package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public class StagedParticipantServiceImpl implements StagedParticipantService {
	private static final Log logger = LogFactory.getLog(StagedParticipantServiceImpl.class);

	private DaoFactory daoFactory;

	private ParticipantService participantSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setparticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StagedParticipantDetail> saveOrUpdateParticipant(RequestEvent<StagedParticipantDetail> req) {
		try {
			StagedParticipantDetail detail = req.getPayload();
			updateParticipantIfExists(detail);
			StagedParticipant savedParticipant = saveOrUpdateParticipant(getMatchingParticipant(detail), detail);
			return ResponseEvent.response(StagedParticipantDetail.from(savedParticipant));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void updateParticipantIfExists(StagedParticipantDetail input) {
		Participant existing = daoFactory.getParticipantDao().getByEmpi(input.getEmpi());

		if (existing == null) {
			return;
		}

		ParticipantDetail detail = ParticipantDetail.from(existing, false);
		detail.setEmpi(input.getEmpi());
		detail.setUid(input.getUid());
		detail.setEthnicities(input.getEthnicities());
		detail.setGender(input.getGender());
		detail.setLastName(input.getLastName());
		detail.setFirstName(input.getFirstName());
		detail.setMiddleName(input.getMiddleName());
		detail.setBirthDate(input.getBirthDate());
		detail.setRaces(input.getRaces());
		detail.setPmis(input.getPmis());
		detail.setSource(input.getSource());

		ResponseEvent<ParticipantDetail> resp = participantSvc.updateParticipant(new RequestEvent<>(detail));
		if (resp.isSuccessful()) {
			logger.info("Matching participant (empi: '" + detail.getEmpi() + "') found and updated!");
		}
	}

	private StagedParticipant getMatchingParticipant(StagedParticipantDetail detail) {
		if (StringUtils.isBlank(detail.getEmpi())) {
			return null;
		}

		return daoFactory.getStagedParticipantDao().getByEmpi(detail.getEmpi());
	}

	private StagedParticipant saveOrUpdateParticipant(StagedParticipant existing, StagedParticipantDetail input) {
		StagedParticipant participant = createParticipant(existing, input);
		if (existing != null) {
			existing.update(participant);
			participant = existing;
		}

		daoFactory.getStagedParticipantDao().saveOrUpdate(participant);
		return participant;
	}

	private StagedParticipant createParticipant(StagedParticipant existing, StagedParticipantDetail detail) {
		StagedParticipant participant = new StagedParticipant();
		if (existing != null) {
			BeanUtils.copyProperties(existing, participant);
		}

		setParticipantAtrrs(detail, participant);
		return participant;
	}

	private void setParticipantAtrrs(StagedParticipantDetail detail, StagedParticipant participant) {
		participant.setFirstName(detail.getFirstName());
		participant.setLastName(detail.getLastName());
		participant.setBirthDate(detail.getBirthDate());
		participant.setGender(getPv(PvAttributes.GENDER, detail.getGender(), ParticipantErrorCode.INVALID_GENDER));
		participant.setVitalStatus(getPv(PvAttributes.VITAL_STATUS, detail.getVitalStatus(), ParticipantErrorCode.INVALID_VITAL_STATUS));
		participant.setUpdatedTime(Calendar.getInstance().getTime());

		if (StringUtils.isNotBlank(detail.getSource())) {
			participant.setSource(detail.getSource());
		}

		if (StringUtils.isNotBlank(detail.getNewEmpi())) {
			participant.setEmpi(detail.getNewEmpi());
			participant.getPmiList().addAll(getPmis(detail.getPmis()));
		} else {
			participant.setEmpi(detail.getEmpi());
			participant.setPmiList(getPmis(detail.getPmis()));

		}

		Set<String> races = detail.getRaces();
		if (CollectionUtils.isNotEmpty(races)) {
			participant.setRaces(getPvs(PvAttributes.RACE, races, ParticipantErrorCode.INVALID_RACE));
		}

		Set<String> ethnicities = detail.getEthnicities();
		if (CollectionUtils.isNotEmpty(ethnicities)) {
			participant.setRaces(getPvs(PvAttributes.ETHNICITY, ethnicities, ParticipantErrorCode.INVALID_ETHNICITY));
		}
	}

	private Set<StagedParticipantMedicalIdentifier> getPmis(List<PmiDetail> pmis) {
		if (CollectionUtils.isEmpty(pmis)) {
			return Collections.emptySet();
		}

		return pmis.stream().map(
			id -> {
				StagedParticipantMedicalIdentifier pmi = new StagedParticipantMedicalIdentifier();
				pmi.setSite(id.getSiteName());
				pmi.setMedicalRecordNumber(id.getMrn());
				return pmi;
			}
		).collect(Collectors.toSet());
	}

	public PermissibleValue getPv(String attr, String value, ErrorCode invErrorCode) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, value);
		if (pv == null) {
			throw OpenSpecimenException.userError(invErrorCode);
		}

		return pv;
	}

	public Set<PermissibleValue> getPvs(String attr, Collection<String> values, ErrorCode invErrorCode) {
		if (CollectionUtils.isEmpty(values)) {
			return new HashSet<>();
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(attr, values);
		if (pvs.size() != values.size()) {
			throw OpenSpecimenException.userError(invErrorCode);
		}

		return new HashSet<>(pvs);
	}
}
