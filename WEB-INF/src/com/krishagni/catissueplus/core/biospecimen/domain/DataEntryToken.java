package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;

public interface DataEntryToken {
	enum Status {
		PENDING,

		COMPLETED,

		EXPIRED
	}

	Long getId();

	String getToken();

	User getCreatedBy();

	Date getCreationTime();

	Date getCompletionTime();

	Date getExpiryTime();

	Status getStatus();

	String getUrl();
}
