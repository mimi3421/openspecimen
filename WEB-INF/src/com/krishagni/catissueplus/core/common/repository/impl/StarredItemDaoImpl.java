package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.StarredItem;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.StarredItemDao;

public class StarredItemDaoImpl extends AbstractDao<StarredItem> implements StarredItemDao {
	@Override
	public Class<StarredItem> getType() {
		return StarredItem.class;
	}

	@Override
	public StarredItem getItem(String itemType, Long itemId, Long userId) {
		return (StarredItem) getCurrentSession().createCriteria(StarredItem.class, "si")
			.createAlias("si.user", "user")
			.add(Restrictions.eq("si.itemType", itemType))
			.add(Restrictions.eq("si.itemId", itemId))
			.add(Restrictions.eq("user.id", userId))
			.uniqueResult();
	}

	@Override
	public List<Long> getItemIds(String itemType, Long userId) {
		List<Object> rows = getCurrentSession().createCriteria(StarredItem.class, "si")
			.createAlias("si.user", "user")
			.setProjection(Projections.property("si.itemId"))
			.add(Restrictions.eq("si.itemType", itemType))
			.add(Restrictions.eq("user.id", userId))
			.list();
		return rows.stream().map(itemId -> ((Number) itemId).longValue()).collect(Collectors.toList());
	}
}