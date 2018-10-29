package com.krishagni.catissueplus.core.administrative.events;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ScheduledJobListCriteria extends AbstractListCriteria<ScheduledJobListCriteria> {

	private ScheduledJob.Type type;

	public ScheduledJob.Type type() {
		return type;
	}

	public ScheduledJobListCriteria type(ScheduledJob.Type type) {
		this.type = type;
		return self();
	}

	@Override
	public ScheduledJobListCriteria self() {
		return this;
	}
}
