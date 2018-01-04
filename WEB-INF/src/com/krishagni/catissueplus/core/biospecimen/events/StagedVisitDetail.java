package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.HashSet;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class StagedVisitDetail extends VisitDetail {
	private Date updatedTime;

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public static StagedVisitDetail from(StagedVisit visit) {
		StagedVisitDetail detail = new StagedVisitDetail();
		detail.setName(visit.getName());
		detail.setSurgicalPathologyNumber(visit.getSurgicalPathologyNumber());
		detail.setVisitDate(visit.getVisitDate());
		detail.setClinicalDiagnoses(new HashSet<>(visit.getClinicalDiagnoses()));
		detail.setClinicalStatus(visit.getClinicalStatus());
		detail.setCohort(visit.getCohort());
		detail.setSite(visit.getCollectionSite());
		detail.setStatus(visit.getStatus());
		detail.setMissedReason(visit.getMissedReason());
		detail.setComments(visit.getComments());
		detail.setUpdatedTime(visit.getUpdateTime());
		return detail;
	}
}
