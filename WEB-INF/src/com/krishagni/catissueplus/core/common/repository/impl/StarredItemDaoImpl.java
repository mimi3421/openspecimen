package com.krishagni.catissueplus.core.common.repository.impl;

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
}
