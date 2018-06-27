package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.events.SpecimenRequestSummary;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestDao;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestListCriteria;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenRequestDaoImpl extends AbstractDao<SpecimenRequest> implements SpecimenRequestDao {
	public Class<SpecimenRequest> getType() {
		return SpecimenRequest.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SpecimenRequestSummary> getSpecimenRequests(SpecimenRequestListCriteria crit) {
		Criteria query = addSummaryFields(getListQuery(crit), CollectionUtils.isNotEmpty(crit.siteIds()));
		return ((List<Object[]>)query.list()).stream().map(this::getRequest).collect(Collectors.toList());
	}

	private Criteria getListQuery(SpecimenRequestListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(SpecimenRequest.class)
			.createAlias("dp", "dp", JoinType.LEFT_OUTER_JOIN)
			.createAlias("requestor", "requestor", JoinType.LEFT_OUTER_JOIN)
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("id"));

		addCatalogCond(query, crit);
		addDateConds(query, crit);
		addStatusConds(query, crit);
		return addUserRestrictions(query, crit);
	}

	private Criteria addCatalogCond(Criteria query, SpecimenRequestListCriteria crit) {
		if (crit.catalogId() == null) {
			return query;
		}

		query.add(Restrictions.eq("catalogId", crit.catalogId()));
		return query;
	}


	private Criteria addDateConds(Criteria query, SpecimenRequestListCriteria crit) {
		if (crit.fromReqDate() != null) {
			query.add(Restrictions.ge("dateOfRequest", Utility.chopTime(crit.fromReqDate())));
		}

		if (crit.toReqDate() != null) {
			query.add(Restrictions.le("dateOfRequest", Utility.getEndOfDay(crit.toReqDate())));
		}

		return query;
	}

	private Criteria addStatusConds(Criteria query, SpecimenRequestListCriteria crit) {
		if (Boolean.TRUE.equals(crit.pendingReqs())) {
			query.add(Restrictions.eq("activityStatus", "Active"));
		}

		if (Boolean.TRUE.equals(crit.closedReqs())) {
			query.add(Restrictions.eq("activityStatus", "Closed"));
		}

		return query;
	}

	private Criteria addUserRestrictions(Criteria query, SpecimenRequestListCriteria crit) {
		Disjunction orCond = Restrictions.disjunction();

		if (CollectionUtils.isNotEmpty(crit.siteIds())) {
			query.createAlias("dp.distributingSites", "distSites", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distSites.site", "distSite", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distSites.institute", "distInst", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distInst.sites", "instSite", JoinType.LEFT_OUTER_JOIN);

			orCond.add(
				Restrictions.and(
					Restrictions.or(
						Restrictions.and(
							Restrictions.isNull("distSites.site"),
							Restrictions.in("instSite.id", crit.siteIds())
						),
						Restrictions.and(
							Restrictions.isNotNull("distSites.site"),
							Restrictions.in("distSite.id", crit.siteIds())
						)
					),
					Restrictions.eq("screeningStatus", SpecimenRequest.ScreeningStatus.APPROVED)
				)
			);
		}

		if (crit.requestorId() != null) {
			orCond.add(Restrictions.eq("requestor.id", crit.requestorId()));
		}

		return query.add(orCond);
	}

	private Criteria addSummaryFields(Criteria query, boolean distinct) {
		ProjectionList projs = Projections.projectionList()
			.add(Projections.property("id"))
			.add(Projections.property("catalogId"))
			.add(Projections.property("requestor.id"))
			.add(Projections.property("requestor.firstName"))
			.add(Projections.property("requestor.lastName"))
			.add(Projections.property("requestor.emailAddress"))
			.add(Projections.property("requestorEmailId"))
			.add(Projections.property("irbId"))
			.add(Projections.property("dp.id"))
			.add(Projections.property("dp.shortTitle"))
			.add(Projections.property("dateOfRequest"))
			.add(Projections.property("dateOfScreening"))
			.add(Projections.property("screeningStatus"))
			.add(Projections.property("activityStatus"));

		query.setProjection(distinct ? Projections.distinct(projs) : projs);
		return query;
	}

	private SpecimenRequestSummary getRequest(Object[] row) {
		int idx = 0;
		SpecimenRequestSummary req = new SpecimenRequestSummary();
		req.setId((Long)row[idx++]);
		req.setCatalogId((Long)row[idx++]);

		UserSummary requestor = new UserSummary();
		requestor.setId((Long)row[idx++]);
		requestor.setFirstName((String)row[idx++]);
		requestor.setLastName((String)row[idx++]);
		requestor.setEmailAddress((String)row[idx++]);
		if (requestor.getId() != null) {
			req.setRequestor(requestor);
		}

		req.setRequestorEmailId((String)row[idx++]);
		req.setIrbId((String)row[idx++]);
		req.setDpId((Long)row[idx++]);
		req.setDpShortTitle((String)row[idx++]);
		req.setDateOfRequest((Date)row[idx++]);
		req.setDateOfScreening((Date)row[idx++]);

		SpecimenRequest.ScreeningStatus status = (SpecimenRequest.ScreeningStatus)row[idx++];
		req.setScreeningStatus(status != null ? status.name() : null);
		req.setActivityStatus((String)row[idx++]);
		return req;
	}
}