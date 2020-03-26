
package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import static com.krishagni.catissueplus.core.common.PvAttributes.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantUtil;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;


public class ParticipantFactoryImpl implements ParticipantFactory {
	private static final String DEAD_STATUS = "Dead";

	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public Participant createParticipant(ParticipantDetail detail) {		
		Participant participant = new Participant();
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setParticipantAttrs(detail, participant, false, ose);
		
		ose.checkAndThrow();
		return participant;
	}
	
	@Override
	public Participant createParticipant(Participant existing, ParticipantDetail detail) {
		existing.setCpId(detail.getCpId());

		Participant participant = new Participant();
		BeanUtils.copyProperties(existing, participant, "cprs", "source");
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setParticipantAttrs(detail, participant, true, ose);
		
		ose.checkAndThrow();
		return participant;
		
	}
	
	private void setParticipantAttrs(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (participant.getId() == null && detail.getId() != null && detail.getId() != -1L) {
			participant.setId(detail.getId());
		}

		setSource(detail, participant, partial, ose);
		setUid(detail, participant, partial, ose);
		setEmpi(detail, participant, partial, ose);
		setName(detail, participant, partial, ose);
		setEmailAddress(detail, participant, partial, ose);
		setVitalStatus(detail, participant, partial, ose);
		setBirthAndDeathDate(detail, participant, partial, ose);
		setActivityStatus(detail, participant, partial, ose);
		setGender(detail, participant, partial, ose);
		setRace(detail, participant, partial, ose);
		setEthnicity(detail, participant, partial, ose);
		setPmi(detail, participant, partial, ose);
		setExtension(detail, participant, partial, ose);
	}

	private void setSource(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (partial && !detail.isAttrModified("source")) {
			return;
		}

		participant.setSource(detail.getSource());
	}

	private void setUid(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("uid")) {
			return;
		}
		
		String uid = detail.getUid();
		if (StringUtils.isBlank(uid)) {
			if (ConfigUtil.getInstance().getBoolSetting("biospecimen", "uid_mandatory", false)) {
				oce.addError(ParticipantErrorCode.UID_REQUIRED);
			} else {
				participant.setUid(null);
			}
			return;
		}
		
		if (!ParticipantUtil.isValidUid(uid, oce)) {
			return;
		}
		
		if (partial && !uid.equals(participant.getUid())) {
			ParticipantUtil.ensureUniqueUid(daoFactory, uid, oce);
		}
		
		participant.setUid(uid);
	}
	
	private void setEmpi(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (partial && !detail.isAttrModified("empi")) {
			return;
		}
		
		String empi = detail.getEmpi();
		if (StringUtils.isBlank(empi)) {
			participant.setEmpi(null);
			return;
		}

		if (!ParticipantUtil.isValidMpi(empi, ose)) {
			return;
		}
		
		if (partial && !empi.equals(participant.getEmpi())) {
			//ParticipantUtil.en
		}
		
		participant.setEmpi(empi);
	}
	
	private void setName(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (!partial || detail.isAttrModified("firstName")) {
			participant.setFirstName(detail.getFirstName());
		}
		
		if (!partial || detail.isAttrModified("middleName")) {
			participant.setMiddleName(detail.getMiddleName());
		}
		
		if (!partial || detail.isAttrModified("lastName")) {
			participant.setLastName(detail.getLastName());
		}		
	}

	private void setEmailAddress(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		if (!partial || detail.isAttrModified("emailAddress")) {
			if (StringUtils.isNotBlank(detail.getEmailAddress()) && !Utility.isValidEmail(detail.getEmailAddress())) {
				ose.addError(ParticipantErrorCode.INVALID_EMAIL_ID, detail.getEmailAddress());
			}

			participant.setEmailAddress(detail.getEmailAddress());
		}
	}

	private void setVitalStatus(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("vitalStatus")) {
			return;
		}

		String vitalStatus = detail.getVitalStatus();
		participant.setVitalStatus(getPv(VITAL_STATUS, vitalStatus, ParticipantErrorCode.INVALID_VITAL_STATUS, oce));
	}

	private void setBirthAndDeathDate(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (!partial || detail.isAttrModified("birthDate")) {
			Date birthDate = detail.getBirthDate();
			if (birthDate != null && birthDate.after(Calendar.getInstance().getTime())) {
				oce.addError(ParticipantErrorCode.INVALID_BIRTH_DATE);
				return;
			}

			participant.setBirthDate(birthDate);
		}

		if (participant.getVitalStatus() == null || !DEAD_STATUS.equals(participant.getVitalStatus().getValue())) {
			participant.setDeathDate(null);
		} else if (!partial || detail.isAttrModified("deathDate")) {
			Date deathDate = detail.getDeathDate();

			if (deathDate != null && deathDate.after(Calendar.getInstance().getTime())) {
				oce.addError(ParticipantErrorCode.INVALID_DEATH_DATE);
			}

			participant.setDeathDate(deathDate);
		}

		if (participant.getBirthDate() != null && participant.getDeathDate() != null &&
			participant.getBirthDate().after(participant.getDeathDate())) {
			oce.addError(ParticipantErrorCode.INVALID_DEATH_DATE);
		}
	}

	private void setActivityStatus(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("activityStatus")) {
			return;
		}
				
		String status = detail.getActivityStatus();		
		if (StringUtils.isBlank(status)) {
			participant.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}
		
		if (!Status.isValidActivityStatus(status)) {
			oce.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		participant.setActivityStatus(status);		
	}

	private void setGender(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("gender")) {
			return;
		}
		
		String gender = detail.getGender();
		participant.setGender(getPv(GENDER, gender, ParticipantErrorCode.INVALID_GENDER, oce));
	}

	private void setRace(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("races")) {
			return;
		}
		
		Set<String> races = detail.getRaces();
		if (races == null) {
			return;
		}

		participant.setRaces(getPvs(RACE, races, ParticipantErrorCode.INVALID_RACE, oce));
	}

	private void setEthnicity(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException oce) {
		if (partial && !detail.isAttrModified("ethnicities")) {
			return;
		}
		
		Set<String> ethnicities = detail.getEthnicities();
		if (ethnicities == null) {
			return;
		}

		participant.setEthnicities(getPvs(ETHNICITY, ethnicities, ParticipantErrorCode.INVALID_ETHNICITY, oce));
	}
	
	private void setPmi(
			ParticipantDetail detail,
			Participant participant, 	
			boolean partial,
			OpenSpecimenException oce) {
		
		if (partial && !detail.isAttrModified("pmis")) {
			return;
		}

		if (partial) {
			boolean unique = ParticipantUtil.ensureUniquePmis(
					daoFactory, 
					detail.getPmis(), 
					participant, 
					oce);
			if (!unique) {
				return;
			}
		}
		
		Set<ParticipantMedicalIdentifier> newPmis = new HashSet<>();
		if (CollectionUtils.isEmpty(detail.getPmis())) {
			participant.setPmis(newPmis);
		} else {
			Set<String> siteNames = new HashSet<>();
			boolean dupSite = false;
			
			for (PmiDetail pmiDetail : detail.getPmis()) {
				ParticipantMedicalIdentifier pmi = getPmi(pmiDetail, oce);
				if (pmi == null) {
					continue;
				}
				
				if (!dupSite && !siteNames.add(pmiDetail.getSiteName())) {
					dupSite = true;
					oce.addError(ParticipantErrorCode.DUP_MRN_SITE, pmiDetail.getSiteName());
				}
				
				pmi.setParticipant(participant);
				newPmis.add(pmi);
			}			
		}
				
		participant.setPmis(newPmis);
	}

	private ParticipantMedicalIdentifier getPmi(PmiDetail pmiDetail, OpenSpecimenException oce) {
		if (StringUtils.isBlank(pmiDetail.getSiteName()) && StringUtils.isBlank(pmiDetail.getMrn())) {
			return null;
		}

		Site site = daoFactory.getSiteDao().getSiteByName(pmiDetail.getSiteName());		
		if (site == null) {
			oce.addError(SiteErrorCode.NOT_FOUND);
			return null;
		}
		
		ParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifier();
		pmi.setSite(site);
		pmi.setMedicalRecordNumber(pmiDetail.getMrn());
		return pmi;
	}
	
	private void setExtension(ParticipantDetail detail, Participant participant, boolean partial, OpenSpecimenException ose) {
		participant.setCpId(detail.getCpId());

		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), participant);
		participant.setExtension(extension);
	}

	private PermissibleValue getPv(String attr, String value, ErrorCode invErrorCode, OpenSpecimenException ose) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, value);
		if (pv == null) {
			ose.addError(invErrorCode);
		}

		return pv;
	}

	private Set<PermissibleValue> getPvs(String attr, Collection<String> values, ErrorCode invErrorCode, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(values)) {
			return new HashSet<>();
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(attr, values);
		if (pvs.size() != values.size()) {
			ose.addError(invErrorCode);
		}

		return new HashSet<>(pvs);
	}
}
