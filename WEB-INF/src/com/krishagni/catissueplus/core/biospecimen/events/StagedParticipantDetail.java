package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class StagedParticipantDetail extends ParticipantDetail {

	private Date updatedTime;

	private String newEmpi;

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public String getNewEmpi() {
		return newEmpi;
	}

	public void setNewEmpi(String newEmpi) {
		this.newEmpi = newEmpi;
	}

	public static StagedParticipantDetail from(StagedParticipant participant) {
		StagedParticipantDetail result = new StagedParticipantDetail();
		result.setFirstName(participant.getFirstName());
		result.setLastName(participant.getLastName());
		result.setMiddleName(participant.getMiddleName());
		result.setActivityStatus(participant.getActivityStatus());
		result.setBirthDate(participant.getBirthDate());
		result.setDeathDate(participant.getDeathDate());
		result.setGender(participant.getGender());
		result.setEmpi(participant.getEmpi());
		result.setSexGenotype(participant.getSexGenotype());
		result.setUid(participant.getUid());
		result.setVitalStatus(participant.getVitalStatus());

		result.setPmis(participant.getPmiList().stream().map(pmi -> {
			PmiDetail pmiDetail = new PmiDetail();
			pmiDetail.setSiteName(pmi.getSite());
			pmiDetail.setMrn(pmi.getMedicalRecordNumber());
			return pmiDetail;
		}).collect(Collectors.toList()));


		if (CollectionUtils.isNotEmpty(participant.getRaces())) {
			result.setRaces(new HashSet<>(participant.getRaces()));
		}

		if (CollectionUtils.isNotEmpty(participant.getEthnicities())) {
			result.setEthnicities(new HashSet<>(participant.getEthnicities()));
		}

		result.setUpdatedTime(participant.getUpdatedTime());
		result.setSource(participant.getSource());
		result.setStagedId(participant.getId());
		return result;
	}
}
