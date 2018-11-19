package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.events.MessageLogCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.MessageLogDao;

public class MessageLogDaoImpl extends AbstractDao<MessageLog> implements MessageLogDao {

	@Override
	public Class<MessageLog> getType() {
		return MessageLog.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MessageLog> getMessages(MessageLogCriteria crit) {
		return getQuery(crit).addOrder(Order.asc("log.id"))
			.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public long getMessagesCount(MessageLogCriteria crit) {
		return ((Number) getQuery(crit).setProjection(Projections.rowCount()).uniqueResult()).longValue();
	}

	@Override
	public int deleteOldMessages(Date olderThanDt) {
		return getCurrentSession().getNamedQuery(DELETE_OLD_MSGS)
			.setParameter("olderThanDt", olderThanDt)
			.executeUpdate();
	}

	private Criteria getQuery(MessageLogCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(MessageLog.class, "log");

		if (StringUtils.isNotBlank(crit.externalApp())) {
			query.add(Restrictions.eq("log.externalApp", crit.externalApp()));
		}

		if (CollectionUtils.isNotEmpty(crit.msgTypes())) {
			query.add(Restrictions.in("log.type", crit.msgTypes()));
		}

		if (crit.fromDate() != null) {
			query.add(Restrictions.ge("log.processTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(Restrictions.le("log.processTime", crit.toDate()));
		}

		if (crit.maxRetries() != null) {
			query.add(
				Restrictions.or(
					Restrictions.isNull("log.noOfRetries"),
					Restrictions.lt("log.noOfRetries", crit.maxRetries())
				)
			);
		}

		if (crit.processStatus() != null) {
			query.add(Restrictions.eq("log.processStatus", crit.processStatus()));
		}

		return query;
	}

	private static final String FQN = MessageLog.class.getName();

	private static final String DELETE_OLD_MSGS = FQN + ".deleteOldMessages";
}
