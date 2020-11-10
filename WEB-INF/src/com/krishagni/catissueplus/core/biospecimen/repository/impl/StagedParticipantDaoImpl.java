package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StagedParticipantDaoImpl extends AbstractDao<StagedParticipant> implements StagedParticipantDao {

	@Override
	public Class<StagedParticipant> getType() {
		return StagedParticipant.class;
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<StagedParticipant> getByPmis(List<PmiDetail> pmis) {
		DetachedCriteria subQuery = getByPmisQuery(pmis);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return getCurrentSession().createCriteria(StagedParticipant.class, "sp")
			.add(Subqueries.propertyIn("sp.id", subQuery))
			.list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public StagedParticipant getByEmpi(String empi) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(Restrictions.eq("sp.empi", empi));
		} else {
			query.add(Restrictions.eq("sp.empi", empi).ignoreCase());
		}

		return (StagedParticipant) query.uniqueResult();
	}

	@Override
	public StagedParticipant getByUid(String uid) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(Restrictions.eq("sp.uid", uid));
		} else {
			query.add(Restrictions.eq("sp.uid", uid).ignoreCase());
		}

		return (StagedParticipant) query.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StagedParticipant> getByMrn(String mrn) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp")
			.createAlias("sp.pmiList", "pmi");
		if (isMySQL()) {
			query.add(Restrictions.eq("pmi.medicalRecordNumber", mrn));
		} else {
			query.add(Restrictions.eq("pmi.medicalRecordNumber", mrn).ignoreCase());
		}

		return query.list();
	}

	@Override
	public int deleteOldParticipants(int olderThanDays) {
		Date olderThanDt = Date.from(Instant.now().minus(Duration.ofDays(olderThanDays)));
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_PMIS, olderThanDt);
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_RACES, olderThanDt);
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_ETHNICITIES, olderThanDt);
		return deleteOldParticipantRecs(DEL_OLD_PARTICIPANTS, olderThanDt);
	}

	private DetachedCriteria getByPmisQuery(List<PmiDetail> pmis) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StagedParticipant.class)
			.setProjection(Projections.distinct(Projections.property("id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession())
			.createAlias("pmiList", "pmi");

		Disjunction junction = Restrictions.disjunction();
		boolean added = false;
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			SimpleExpression eqMrn  = Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn());
			SimpleExpression eqSite = Restrictions.eq("pmi.site", pmi.getSiteName());
			if (!isMySQL()) {
				eqMrn  = eqMrn.ignoreCase();
				eqSite = eqSite.ignoreCase();
			}

			junction.add(Restrictions.and(eqMrn, eqSite));
			added = true;
		}

		if (added) {
			query.add(junction);
			return detachedCriteria;
		} else {
			return null;
		}
	}

	private int deleteOldParticipantRecs(String query, Date olderThanDt) {
		return getCurrentSession().getNamedQuery(query).setTimestamp("olderThanDt", olderThanDt).executeUpdate();
	}

	private static final String FQN = StagedParticipant.class.getName();

	private static final String DEL_OLD_PARTICIPANTS = FQN + ".deleteOldParticipants";

	private static final String DEL_OLD_PARTICIPANT_PMIS = FQN + ".deleteOldParticipantPmis";

	private static final String DEL_OLD_PARTICIPANT_RACES = FQN + ".deleteOldParticipantRaces";

	private static final String DEL_OLD_PARTICIPANT_ETHNICITIES = FQN + ".deleteOldParticipantEthnicities";
}
