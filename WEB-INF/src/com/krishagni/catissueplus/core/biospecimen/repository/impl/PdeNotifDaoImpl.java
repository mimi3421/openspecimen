package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.biospecimen.repository.PdeNotifDao;
import com.krishagni.catissueplus.core.biospecimen.repository.PdeNotifListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class PdeNotifDaoImpl extends AbstractDao<PdeNotif> implements PdeNotifDao {

	@Override
	public Class<PdeNotif> getType() {
		return PdeNotif.class;
	}

	@Override
	public void updateLinkStatus(String type, Long tokenId, String status) {
		getCurrentSession().getNamedQuery(UPDATE_LINK_STATUS)
			.setParameter("status", status)
			.setParameter("formType", type)
			.setParameter("tokenId", tokenId)
			.executeUpdate();
	}

	@Override
	public List<PdeNotif> getPendingNotifs(long lastId, int maxResults) {
		Calendar createdOneDayBefore = Calendar.getInstance();
		createdOneDayBefore.add(Calendar.DATE, -1);

		Calendar expiryAfter6Hours = Calendar.getInstance();
		expiryAfter6Hours.add(Calendar.HOUR_OF_DAY, 6);

		DetachedCriteria pendingNotifIds = DetachedCriteria.forClass(PdeNotif.class, "pn")
			.createAlias("pn.links", "link")
			.add(Restrictions.eq("link.status", "PENDING"))
			.add(Restrictions.lt("pn.creationTime", createdOneDayBefore.getTime()))
			.add(Restrictions.gt("pn.expiryTime", expiryAfter6Hours.getTime()))
			.setProjection(Projections.distinct(Projections.property("pn.id")));

		return getCurrentSession().createCriteria(PdeNotif.class, "pn")
			.add(Subqueries.propertyIn("id", pendingNotifIds))
			.add(Restrictions.gt("id", lastId))
			.addOrder(Order.asc("id"))
			.setMaxResults(maxResults)
			.list();
	}

	@Override
	public List<PdeNotif> getNotifs(PdeNotifListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(PdeNotif.class, "pn")
			.createAlias("pn.links", "link");

		boolean cprAliasAdded = false;
		if (StringUtils.isNotBlank(crit.ppid())) {
			query.createAlias("pn.cpr", "cpr")
				.add(Restrictions.ilike("cpr.ppid", crit.ppid()));
			cprAliasAdded = true;
		}

		if (crit.cpId() != null) {
			if (!cprAliasAdded) {
				query.createAlias("pn.cpr", "cpr");
				cprAliasAdded = true;
			}

			query.createAlias("cpr.collectionProtocol", "cp")
				.add(Restrictions.eq("cp.id", crit.cpId()));
		}

		if (crit.minCreationTime() != null) {
			query.add(Restrictions.ge("pn.creationTime", crit.minCreationTime()));
		}

		if (crit.maxCreationTime() != null) {
			query.add(Restrictions.le("pn.creationTime", crit.maxCreationTime()));
		}

		if (StringUtils.isNotBlank(crit.status())) {
			switch (crit.status().toLowerCase()) {
				case "completed":
					query.add(Restrictions.eq("link.status", "COMPLETED"));
					break;

				case "pending":
					query.add(Restrictions.eq("link.status", "PENDING"))
						.add(Restrictions.gt("pn.expiryTime", Calendar.getInstance().getTime()));
					break;

				case "expired":
					query.add(Restrictions.eq("link.status", "PENDING"))
						.add(Restrictions.le("pn.expiryTime", Calendar.getInstance().getTime()));
					break;
			}
		}

		return query.addOrder(Order.desc("pn.creationTime"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	private static final String FQN = PdeNotif.class.getName();

	private static final String UPDATE_LINK_STATUS = FQN + ".updateLinkStatus";
}
