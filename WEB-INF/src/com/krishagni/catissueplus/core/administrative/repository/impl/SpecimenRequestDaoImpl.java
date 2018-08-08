package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.events.SpecimenRequestSummary;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestDao;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestListCriteria;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
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
		Criteria query = addSummaryFields(getListQuery(crit), CollectionUtils.isNotEmpty(crit.sites()));
		return ((List<Object[]>)query.list()).stream().map(this::getRequest).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getRequestIds(String key, Object value) {
		List<Object[]> rows = getCurrentSession().createCriteria(SpecimenRequest.class)
			.add(Restrictions.eq(key, value))
			.setProjection(Projections.projectionList()
				.add(Projections.property("id"))
				.add(Projections.property("catalogId")))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("requestId", row[0]);
		ids.put("catalogId", row[1]);
		return ids;
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
		if (StringUtils.isBlank(crit.status())) {
			return query;
		}

		if (crit.status().equals("PROCESSED")) {
			//
			// processed requests are those that are approved and closed
			//
			query.add(Restrictions.eq("screeningStatus", SpecimenRequest.ScreeningStatus.APPROVED))
				.add(Restrictions.eq("activityStatus", "Closed"));
		} else {
			try {
				SpecimenRequest.ScreeningStatus status = SpecimenRequest.ScreeningStatus.valueOf(crit.status());
				if (status == SpecimenRequest.ScreeningStatus.APPROVED) {
					//
					// we need to add this condition to filter out the processed requests
					// as they are also approved requests
					//
					query.add(Restrictions.eq("activityStatus", "Active"));
				}

				query.add(Restrictions.eq("screeningStatus", status));
			} catch (Exception e) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, crit.status());
			}
		}

		return query;
	}

	private Criteria addUserRestrictions(Criteria query, SpecimenRequestListCriteria crit) {
		Disjunction orCond = Restrictions.disjunction();

		if (CollectionUtils.isNotEmpty(crit.sites())) {
			Set<Long> instituteIds = new HashSet<>();
			Set<Long> siteIds      = new HashSet<>();
			for (SiteCpPair site : crit.sites()) {
				if (site.getSiteId() != null) {
					siteIds.add(site.getSiteId());
				} else if (site.getInstituteId() != null) {
					instituteIds.add(site.getInstituteId());
				}
			}

			query.createAlias("dp.distributingSites", "distSites", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distSites.site", "distSite", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distSites.institute", "distInst", JoinType.LEFT_OUTER_JOIN)
				.createAlias("distInst.sites", "instSite", JoinType.LEFT_OUTER_JOIN);

			Disjunction instituteConds = Restrictions.disjunction();
			if (!siteIds.isEmpty()) {
				instituteConds.add(Restrictions.in("instSite.id", siteIds));
			}

			if (!instituteIds.isEmpty()) {
				instituteConds.add(Restrictions.in("distInst.id", instituteIds));
			}

			Disjunction siteConds = Restrictions.disjunction();
			if (!siteIds.isEmpty()) {
				siteConds.add(Restrictions.in("distSite.id", siteIds));
			}

			if (!instituteIds.isEmpty()) {
				DetachedCriteria instituteSites = DetachedCriteria.forClass(Site.class)
					.add(Restrictions.in("institute.id", instituteIds))
					.setProjection(Projections.property("id"));
				siteConds.add(Subqueries.propertyIn("distSite.id", instituteSites));
			}

			orCond.add(
				Restrictions.and(
					Restrictions.or(
						Restrictions.and(Restrictions.isNull("distSites.site"), instituteConds),
						Restrictions.and(Restrictions.isNotNull("distSites.site"), siteConds)
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