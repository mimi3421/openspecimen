package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.ContainerCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;

@Configurable
public class LeastEmptyContainerSelectionStrategy implements ContainerSelectionStrategy {
	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public StorageContainer getContainer(ContainerCriteria criteria, Boolean aliquotsInSameContainer) {
		criteria.reservedLaterThan(ignoreReservationsBeforeDate()).numContainers(1);
		List<Long> containerIds = getLeastEmptyContainerId(criteria, aliquotsInSameContainer);
		if (CollectionUtils.isEmpty(containerIds)) {
			return null;
		}

		return daoFactory.getStorageContainerDao().getById(containerIds.get(0));
	}

	@SuppressWarnings("unchecked")
	private List<Long> getLeastEmptyContainerId(ContainerCriteria crit, Boolean aliquotsInSameContainer) {
		sessionFactory.getCurrentSession().flush();

		String sql = sessionFactory.getCurrentSession().getNamedQuery(GET_LEAST_EMPTY_CONTAINER_ID).getQueryString();
		int groupByIdx = sql.indexOf("group by");
		String beforeGroupBySql = sql.substring(0, groupByIdx);
		String groupByLaterSql  = sql.substring(groupByIdx);
		sql = beforeGroupBySql;

		List<String> accessRestrictions = new ArrayList<>();
		for (SiteCpPair siteCp : crit.siteCps()) {
			accessRestrictions.add(new StringBuilder("(c.site_id = ")
				.append(siteCp.getSiteId())
				.append(" and ")
				.append("(allowed_cps.cp_id is null or allowed_cps.cp_id = ").append(siteCp.getCpId()).append(")")
				.append(")")
				.toString()
			);
		}
		sql += " and (" + StringUtils.join(accessRestrictions, " or ") + ") ";

		if (crit.rule() != null) {
			sql += " and (" + crit.rule().getSql("c", crit.ruleParams()) + ") ";
		}

		sql += groupByLaterSql;
		return sessionFactory.getCurrentSession().createSQLQuery(sql)
			.addScalar("containerId", LongType.INSTANCE)
			.setLong("cpId", crit.specimen().getCpId())
			.setString("specimenClass", crit.specimen().getSpecimenClass())
			.setString("specimenType", crit.specimen().getType())
			.setInteger("minFreeLocs", crit.getRequiredPositions(aliquotsInSameContainer))
			.setDate("reservedLaterThan", crit.reservedLaterThan())
			.setMaxResults(crit.numContainers())
			.list();
	}

	private Date ignoreReservationsBeforeDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -5);
		return cal.getTime();
	}

	private static final String GET_LEAST_EMPTY_CONTAINER_ID = StorageContainer.class.getName() + ".getLeastEmptyContainerId";
}