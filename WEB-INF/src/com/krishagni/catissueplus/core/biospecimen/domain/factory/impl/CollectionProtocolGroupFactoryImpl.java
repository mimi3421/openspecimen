package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolGroupFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class CollectionProtocolGroupFactoryImpl implements CollectionProtocolGroupFactory {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public CollectionProtocolGroup createGroup(CollectionProtocolGroupDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		CollectionProtocolGroup group = new CollectionProtocolGroup();
		group.setId(input.getId());
		setName(input, group, ose);
		setCps(input, group, ose);
		ose.checkAndThrow();

		return group;
	}

	private void setName(CollectionProtocolGroupDetail input, CollectionProtocolGroup group, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(CpGroupErrorCode.NAME_REQ);
		}

		group.setName(input.getName());
	}

	private void setCps(CollectionProtocolGroupDetail input, CollectionProtocolGroup group, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(input.getCps())) {
			ose.addError(CpGroupErrorCode.CP_REQ);
			return;
		}

		List<Long> ids = new ArrayList<>();
		List<String> shortTitles = new ArrayList<>();
		for (CollectionProtocolSummary cp : input.getCps()) {
			if (cp.getId() != null) {
				ids.add(cp.getId());
			} else if (StringUtils.isNotBlank(cp.getShortTitle())) {
				shortTitles.add(cp.getShortTitle());
			}
		}

		if (ids.isEmpty() && shortTitles.isEmpty()) {
			ose.addError(CpGroupErrorCode.CP_REQ);
			return;
		}

		if (!ids.isEmpty()) {
			List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getByIds(ids);
			if (cps.size() != ids.size()) {
				ids.removeAll(cps.stream().map(CollectionProtocol::getId).collect(Collectors.toSet()));
				ose.addError(
					CpGroupErrorCode.CP_NOT_FOUND,
					ids.stream().map(Object::toString).collect(Collectors.joining(",")),
					ids.size());
			} else {
				group.getCps().addAll(cps);
			}
		}

		if (!shortTitles.isEmpty()) {
			List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCpsByShortTitle(shortTitles);
			if (cps.size() != shortTitles.size()) {
				shortTitles.removeAll(cps.stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet()));
				ose.addError(CpGroupErrorCode.CP_NOT_FOUND, String.join(",", shortTitles), shortTitles.size());
			} else {
				group.getCps().addAll(cps);
			}
		}
	}
}
