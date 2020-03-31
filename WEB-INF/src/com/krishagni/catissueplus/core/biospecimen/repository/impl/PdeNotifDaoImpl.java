package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Calendar;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.PdeNotif;
import com.krishagni.catissueplus.core.biospecimen.repository.PdeNotifDao;
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

	private static final String FQN = PdeNotif.class.getName();

	private static final String UPDATE_LINK_STATUS = FQN + ".updateLinkStatus";
}
