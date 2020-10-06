package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Shipment.Status;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSpecimen;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ShipmentDao;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ShipmentDaoImpl extends AbstractDao<Shipment> implements ShipmentDao {

	@Override
	public Class<Shipment> getType() {
		return Shipment.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Shipment> getShipments(ShipmentListCriteria crit) {
		return getShipmentsQuery(crit)
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.desc("shippedDate"))
			.list();
	}

	@Override
	public Long getShipmentsCount(ShipmentListCriteria crit) {
		Number count = (Number) getShipmentsQuery(crit)
				.setProjection(Projections.rowCount())
				.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Shipment getShipmentByName(String name) {
		List<Shipment> result = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_SHIPMENT_BY_NAME)
			.setString("name", name)
			.list();
		
		return result.isEmpty() ? null : result.iterator().next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Specimen> getShippedSpecimensByIds(List<Long> specimenIds) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_SHIPPED_SPECIMENS_BY_IDS)
			.setParameterList("ids", specimenIds)
			.list();
	}

	@Override
	public Map<String, Object> getShipmentIds(String key, Object value) {
		return getObjectIds("shipmentId", key, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ShipmentContainer> getShipmentContainers(ShipmentItemsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ShipmentContainer.class, "sc")
			.createAlias("sc.shipment", "s")
			.add(Restrictions.eq("s.id", crit.shipmentId()))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults());

		if (StringUtils.isNotBlank(crit.orderBy())) {
			if ("name".equals(crit.orderBy())) {
				query.createAlias("sc.container", "box")
					.addOrder(crit.asc() ? Order.asc("box.name") : Order.desc("box.name"));
			}
		}

		return query.addOrder(Order.asc("sc.id")).list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ShipmentSpecimen> getShipmentSpecimens(ShipmentItemsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ShipmentSpecimen.class, "ss")
			.createAlias("ss.shipment", "s")
			.add(Restrictions.eq("s.id", crit.shipmentId()));

		if (crit.containerId() != null) {
			query.createAlias("ss.specimen", "spmn")
				.createAlias("spmn.position", "pos")
				.createAlias("pos.container", "box")
				.createAlias("box.ancestorContainers", "container")
				.add(Restrictions.eq("container.id", crit.containerId()));
		}

		if (StringUtils.isNotBlank(crit.orderBy())) {
			switch (crit.orderBy()) {
				case "label":
					query.createAlias("ss.specimen", "spmn")
						.addOrder(crit.asc() ? Order.asc("spmn.label") : Order.desc("spmn.label"));
					break;

				case "ppid":
					query.createAlias("ss.specimen", "spmn")
						.createAlias("spmn.visit", "visit")
						.createAlias("visit.registration", "cpr")
						.addOrder(crit.asc() ? Order.asc("cpr.ppid") : Order.desc("cpr.ppid"));
					break;

				case "cp":
					query.createAlias("ss.specimen", "spmn")
						.createAlias("spmn.collectionProtocol", "cp")
						.addOrder(crit.asc() ? Order.asc("cp.shortTitle") : Order.desc("cp.shortTitle"));
					break;

				case "location":
					if (crit.containerId() == null) {
						query.createAlias("ss.specimen", "spmn")
							.createAlias("spmn.position", "pos", JoinType.LEFT_OUTER_JOIN)
							.createAlias("pos.container", "box", JoinType.LEFT_OUTER_JOIN);
					}

					query.addOrder(crit.asc() ? Order.asc("box.name") : Order.desc("box.name"))
						.addOrder(crit.asc() ? Order.asc("pos.posTwoOrdinal") : Order.desc("pos.posOneOrdinal"))
						.addOrder(crit.asc() ? Order.asc("pos.posOneOrdinal") : Order.desc("pos.posOneOrdinal"));
					break;
			}
		}

		return query.addOrder(Order.asc("ss.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Integer> getSpecimensCount(Collection<Long> shipmentIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPECIMENS_COUNT)
			.setParameterList("shipmentIds", shipmentIds)
			.list();

		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Integer> getSpecimensCountByContainer(Long shipmentId, Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPECIMENS_COUNT_BY_CONT)
			.setParameter("shipmentId", shipmentId)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	private Criteria getShipmentsQuery(ShipmentListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(Shipment.class)
			.createAlias("sendingSite", "sendSite")
			.createAlias("receivingSite", "recvSite");

		applyIdsFilter(query, "id", crit.ids());
		addNameRestrictions(query, crit);
		addSendSiteRestrictions(query, crit);
		addRecvSiteRestrictions(query, crit);
		addStatusRestrictions(query, crit);
		addSiteRestrictions(query, crit);
		return query;
	}

	private void addNameRestrictions(Criteria query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.name())) {
			return;
		}
		
		query.add(Restrictions.ilike("name", crit.name(), crit.matchMode()));
	}

	private void addSendSiteRestrictions(Criteria query, ShipmentListCriteria crit) {
		if (StringUtils.isBlank(crit.sendingSite())) {
			return;
		}

		query.add(Restrictions.eq("sendSite.name", crit.sendingSite()));
	}

	private void addRecvSiteRestrictions(Criteria query, ShipmentListCriteria crit) {
		if (StringUtils.isNotBlank(crit.recvInstitute())) {
			query.createAlias("recvSite.institute", "institute")
				.add(Restrictions.eq("institute.name", crit.recvInstitute()));
		}

		if (StringUtils.isNotBlank(crit.recvSite())) {
			query.add(Restrictions.eq("recvSite.name", crit.recvSite()));
		}
	}

	private void addStatusRestrictions(Criteria query, ShipmentListCriteria crit) {
		if (crit.status() == null) {
			return;
		}

		query.add(Restrictions.eq("status", crit.status()));
	}

	//
	// Used to restrict access of shipments based on users' roles on various sites
	//
	private void addSiteRestrictions(Criteria query, ShipmentListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.sites())) {
			return;
		}

		Set<Long> instituteIds = new HashSet<>();
		Set<Long> siteIds      = new HashSet<>();
		for (SiteCpPair site : crit.sites()) {
			if (site.getSiteId() != null) {
				siteIds.add(site.getSiteId());
			} else if (site.getInstituteId() != null) {
				instituteIds.add(site.getInstituteId());
			}
		}

		//
		// (recv site is one of accessible sites and shipment is not pending) or (send site is one of accessible sites)
		//
		query.add(
			Restrictions.or(
				Restrictions.and(
					getSiteRestriction("recvSite.id", instituteIds, siteIds),
					Restrictions.ne("status", Status.PENDING)
				), /* end of AND */
				getSiteRestriction("sendSite.id", instituteIds, siteIds)
			) /* end of OR */
		);
	}

	private Criterion getSiteRestriction(String sitePropName, Collection<Long> instituteIds, Collection<Long> siteIds) {
		Disjunction result = Restrictions.disjunction();

		if (!instituteIds.isEmpty()) {
			DetachedCriteria instituteSites = DetachedCriteria.forClass(Site.class)
				.add(Restrictions.in("institute.id", instituteIds))
				.setProjection(Projections.property("id"));
			result.add(Subqueries.propertyIn(sitePropName, instituteSites));
		}

		if (!siteIds.isEmpty()) {
			result.add(Restrictions.in(sitePropName, siteIds));
		}

		return result;
	}
	
	private static final String FQN = Shipment.class.getName();
	
	private static final String GET_SHIPMENT_BY_NAME = FQN + ".getShipmentByName";
	
	private static final String GET_SHIPPED_SPECIMENS_BY_IDS = FQN + ".getShippedSpecimensByIds";

	private static final String GET_SPECIMENS_COUNT = FQN + ".getSpecimensCount";

	private static final String GET_SPECIMENS_COUNT_BY_CONT = FQN + ".getSpecimensCountByContainer";
}
