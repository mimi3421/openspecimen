package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StagedParticipant extends Participant {
	
	private Date updatedTime;
	
	private Set<StagedParticipantMedicalIdentifier> pmiList = new HashSet<>();

	private Set<StagedVisit> visits = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}
	
	public Set<StagedParticipantMedicalIdentifier> getPmiList() {
		return pmiList;
	}

	public void setPmiList(Set<StagedParticipantMedicalIdentifier> pmiList) {
		this.pmiList = pmiList;
	}

	public Set<StagedVisit> getVisits() {
		return visits;
	}

	public void setVisits(Set<StagedVisit> visits) {
		this.visits = visits;
	}

	public void update(StagedParticipant participant) {
		super.update(participant);
		setUpdatedTime(participant.getUpdatedTime());
		updatePmis(participant);
	}

	private void updatePmis(StagedParticipant participant) {
		for (StagedParticipantMedicalIdentifier pmi : participant.getPmiList()) {
			StagedParticipantMedicalIdentifier existing = getPmiBySite(getPmiList(), pmi.getSite());
			if (existing == null) {
				StagedParticipantMedicalIdentifier newPmi = new StagedParticipantMedicalIdentifier();
				newPmi.setParticipant(this);
				newPmi.setSite(pmi.getSite());
				newPmi.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
				getPmiList().add(newPmi);
			} else {
				existing.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
			}
		}

		getPmiList().removeIf(pmi -> (getPmiBySite(participant.getPmiList(), pmi.getSite()) == null));
	}

	private StagedParticipantMedicalIdentifier getPmiBySite(Collection<StagedParticipantMedicalIdentifier> pmis, String siteName) {
		return pmis.stream().filter(pmi -> pmi.getSite().equals(siteName)).findFirst().orElse(null);
	}
}
