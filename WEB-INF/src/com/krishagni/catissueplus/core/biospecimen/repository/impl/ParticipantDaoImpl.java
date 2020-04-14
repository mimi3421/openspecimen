
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.ParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ParticipantDaoImpl extends AbstractDao<Participant> implements ParticipantDao {
	
	@Override
	@SuppressWarnings("unchecked")
	public Participant getByUid(String uid) {		
		List<Participant> participants = getCurrentSession().getNamedQuery(GET_BY_UID)
			.setParameter("uid", uid.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Participant getByEmpi(String empi) {
		List<Participant> participants = getCurrentSession().getNamedQuery(GET_BY_EMPI)
			.setParameter("empi", empi.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}

	@Override
	public Participant getByEmailId(String emailId) {
		return (Participant) getCurrentSession().getNamedQuery(GET_BY_EMAIL_ID)
			.setParameter("emailId", emailId)
			.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<Participant> getByLastNameAndBirthDate(String lname, Date dob) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(dob.toInstant(), ZoneId.systemDefault());
		Date dobStart     = Date.from(zdt.with(LocalTime.MIN).toInstant());
		Date dobEnd       = Date.from(zdt.with(LocalTime.MAX).toInstant());

		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_BY_LNAME_AND_DOB)
				.setString("lname", lname.toLowerCase())
				.setTimestamp("dobStart", dobStart)
				.setTimestamp("dobEnd", dobEnd)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<Participant> getByPmis(List<PmiDetail> pmis) {
		Criteria query = getByPmisQuery(pmis);
		if (query == null) {
			return Collections.emptyList();
		}
		
		return query.list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getParticipantIdsByPmis(List<PmiDetail> pmis) {
		Criteria query = getByPmisQuery(pmis);
		if (query == null) {
			return Collections.emptyList();
		}
		
		ProjectionList projs = Projections.projectionList().add(Projections.property("id"));
		query.setProjection(projs);
		return query.list();
	}
	
	@Override
	public boolean isUidUnique(String uid) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PARTICIPANT_ID_BY_UID);
		query.setString("uid", uid.toLowerCase());
		return query.list().isEmpty();
	}

	@Override
	public boolean isPmiUnique(String siteName, String mrn) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PMI_ID_BY_SITE_MRN);
		query.setString("siteName", siteName.toLowerCase());
		query.setString("mrn", mrn.toLowerCase());
		return query.list().isEmpty();
	}
	
	@Override
	public Class<Participant> getType() {
		return Participant.class;
	}
	
	private Criteria getByPmisQuery(List<PmiDetail> pmis) {
		Criteria query = sessionFactory.getCurrentSession().createCriteria(Participant.class)
				.createAlias("pmis", "pmi")
				.createAlias("pmi.site", "site");

		boolean added = false;
		Disjunction junction = Restrictions.disjunction();
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}
			
			junction.add(
				Restrictions.and(
					Restrictions.eq("site.name", pmi.getSiteName()).ignoreCase(),
					Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn()).ignoreCase()
				)
			);
			
			added = true;
		}
		
		if (!added) {
			return null;
		}
		
		return query.add(junction);
	}

	private static final String FQN = Participant.class.getName();

	private static final String GET_PARTICIPANT_ID_BY_UID = FQN + ".getParticipantIdByUid";

	private static final String GET_PMI_ID_BY_SITE_MRN = FQN + ".getPmiIdBySiteMrn";
	
	private static final String GET_BY_UID = FQN + ".getByUid";
	
	private static final String GET_BY_EMPI = FQN + ".getByEmpi";

	private static final String GET_BY_EMAIL_ID = FQN + ".getByEmailId";
	
	private static final String GET_BY_LNAME_AND_DOB = FQN + ".getByLnameAndDob";
}
